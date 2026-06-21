package com.extremecoffee.app.data

import com.extremecoffee.app.Config
import com.extremecoffee.app.model.AppUser
import com.extremecoffee.app.model.CoffeeEvent
import com.extremecoffee.app.model.InviteResponse
import com.extremecoffee.app.model.ParticipantLocation
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Repository IBRIDO:
 *  - lo stato vive in locale (StateFlow): l'app è istantanea e non crasha mai;
 *  - se Firebase è configurato e raggiungibile, scrive/legge anche da Firestore,
 *    così i telefoni si vedono in tempo reale.
 *  - OGNI accesso a Firebase è in runCatching: un errore non può chiudere l'app.
 */
object CoffeeRepository {

    // Id del progetto segnaposto: se è questo, Firebase NON è configurato -> resta tutto locale.
    private const val PLACEHOLDER_PROJECT = "extreme-coffee-demo"

    private val db: FirebaseFirestore? by lazy {
        try {
            val projectId = FirebaseApp.getInstance().options.projectId
            if (projectId.isNullOrBlank() || projectId == PLACEHOLDER_PROJECT) {
                null // segnaposto -> nessun accesso a Firestore (niente crash)
            } else {
                Firebase.firestore
            }
        } catch (e: Throwable) {
            null
        }
    }

    private val invitedEventsState = MutableStateFlow<List<CoffeeEvent>>(emptyList())
    private val declinedState = MutableStateFlow<Set<String>>(emptySet())
    fun setDeclined(ids: Set<String>) { declinedState.value = ids }
    fun addDeclined(id: String) { declinedState.value = declinedState.value + id }
    fun declineLocally(eventId: String) = addDeclined(eventId)

    private val responsesState = MutableStateFlow<List<InviteResponse>>(emptyList())
    private var responsesReg: ListenerRegistration? = null
    /** Risposte (accetta/rifiuta) agli eventi che HO lanciato io. */
    fun responsesForMe(myId: String): StateFlow<List<InviteResponse>> {
        if (responsesReg == null) {
            runCatching {
                responsesReg = db?.collection("responses")?.whereEqualTo("launcherId", myId)
                    ?.addSnapshotListener { snap, _ ->
                        runCatching {
                            val list = snap?.documents?.mapNotNull { it.toObject<InviteResponse>() }
                            if (list != null) responsesState.value = list
                        }
                    }
            }
        }
        return responsesState
    }

    /** Scrive la risposta dell'invitato (per notificare chi ha lanciato). */
    suspend fun sendResponse(event: CoffeeEvent, fromId: String, fromName: String, status: String) {
        runCatching {
            db?.collection("responses")?.document("${event.id}_$fromId")?.set(
                InviteResponse(event.id, event.launcherId, fromId, fromName, status, System.currentTimeMillis(), event.barName)
            )?.await()
        }
    }

    /** Statistiche personali (caffè totali, del mese, streak, locale preferito) per il loop di retention. */
    suspend fun loadMyStats(context: android.content.Context): MyStats {
        val d = db ?: return MyStats()
        val me = Profile.id(context)
        val launchedTimes = ArrayList<Long>()
        val joinedTimes = ArrayList<Long>()
        val bars = ArrayList<String>()
        var maxDist = 0.0
        // Eventi che ho lanciato io (escludo annullati e simulati)
        runCatching {
            val snap = d.collection("events").whereEqualTo("launcherId", me).get().await()
            for (doc in snap.documents) {
                val ev = doc.toObject<CoffeeEvent>() ?: continue
                if (ev.cancelled) continue
                if (doc.getBoolean("simulated") == true) continue
                if (ev.createdAt > 0) {
                    launchedTimes.add(ev.createdAt)
                    if (ev.barName.isNotBlank()) bars.add(ev.barName)
                }
            }
        }
        // Inviti che HO accettato io (filtro lo status lato client: una sola query per evitare indici)
        runCatching {
            val snap = d.collection("responses").whereEqualTo("fromId", me).get().await()
            for (doc in snap.documents) {
                val r = doc.toObject<InviteResponse>() ?: continue
                if (r.status != "accepted") continue
                if (r.updatedAt > 0) {
                    joinedTimes.add(r.updatedAt)
                    if (r.barName.isNotBlank()) bars.add(r.barName)
                    if (r.distanceKm > maxDist) maxDist = r.distanceKm
                }
            }
        }
        val times = launchedTimes + joinedTimes
        if (times.isEmpty()) return MyStats()
        val now = System.currentTimeMillis()
        val curWeek = weekIndex(now)
        val curMonth = monthKey(now)
        val weeks = times.map { weekIndex(it) }.toSet()
        val streak = computeStreak(weeks, curWeek)
        val month = times.count { monthKey(it) == curMonth }
        val favorite = bars.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: ""
        val atRisk = streak > 0 && !weeks.contains(curWeek)
        return MyStats(
            launched = launchedTimes.size,
            joined = joinedTimes.size,
            total = times.size,
            thisMonth = month,
            streakWeeks = streak,
            atRisk = atRisk,
            favoriteBar = favorite,
            distinctBars = bars.distinct().size,
            maxDistanceKm = maxDist
        )
    }

    /** Registra (merge) la distanza percorsa dal partecipante per il badge "amico vero". */
    suspend fun setParticipationDistanceKm(eventId: String, userId: String, km: Double) {
        runCatching {
            db?.collection("responses")?.document("${eventId}_$userId")
                ?.set(mapOf("distanceKm" to km), SetOptions.merge())?.await()
        }
    }

    private val downloadUrlState = MutableStateFlow(Config.DOWNLOAD_URL)
    private var configReg: ListenerRegistration? = null
    /** Link di download dell'APK: si aggiorna da Firestore (config/app -> downloadUrl). */
    fun downloadUrl(): StateFlow<String> {
        if (configReg == null) {
            runCatching {
                configReg = db?.collection("config")?.document("app")
                    ?.addSnapshotListener { snap, _ ->
                        runCatching {
                            val u = snap?.getString("downloadUrl")
                            if (!u.isNullOrBlank()) downloadUrlState.value = u
                        }
                    }
            }
        }
        return downloadUrlState
    }

    private val eventsState = MutableStateFlow<Map<String, CoffeeEvent>>(emptyMap())
    private val locationsState = MutableStateFlow<Map<String, List<ParticipantLocation>>>(emptyMap())

    private fun putEvent(e: CoffeeEvent) { eventsState.value = eventsState.value + (e.id to e) }
    /** Recupera un evento per id, in modo sincrono (per calcolare la scadenza delle notifiche). */
    fun eventById(id: String): CoffeeEvent? = eventsState.value[id]
    private fun putLocations(id: String, list: List<ParticipantLocation>) {
        locationsState.value = locationsState.value + (id to list)
    }

    suspend fun launchCoffee(event: CoffeeEvent): String {
        val id = UUID.randomUUID().toString()
        val e = event.copy(id = id)
        putEvent(e)
        runCatching { db?.collection("events")?.document(id)?.set(e)?.await() }
        return id
    }

    suspend fun cancelCoffee(eventId: String) {
        // Segna l'evento come annullato (NON lo cancella): la Cloud Function avvisa gli invitati
        // e tutte le viste filtrano gli eventi annullati, quindi sparisce da solo.
        eventsState.value[eventId]?.let { putEvent(it.copy(cancelled = true)) }
        runCatching {
            db?.collection("events")?.document(eventId)
                ?.set(mapOf("cancelled" to true), SetOptions.merge())?.await()
        }
    }

    suspend fun acceptInvite(eventId: String, me: ParticipantLocation) = updateMyLocation(eventId, me)

    suspend fun updateMyLocation(eventId: String, me: ParticipantLocation) {
        val updated = locationsState.value[eventId].orEmpty().filter { it.userId != me.userId } + me
        putLocations(eventId, updated)
        runCatching {
            db?.collection("events")?.document(eventId)
                ?.collection("locations")?.document(me.userId)?.set(me)?.await()
        }
    }

    /** Salva il token FCM del dispositivo per ricevere push anche ad app chiusa. */
    fun saveFcmToken(deviceId: String, token: String, name: String) {
        if (token.isBlank()) return
        runCatching {
            db?.collection("tokens")?.document(deviceId)?.set(
                mapOf("token" to token, "name" to name,
                      "updatedAt" to System.currentTimeMillis())
            )
        }
    }

    /** Elimina account e dati personali: profilo, nickname, token e dati locali. */
    suspend fun deleteMyAccountAndData(context: android.content.Context) {
        val d = db
        val id = Profile.id(context)
        val phone = Profile.phone(context)
        val nick = Profile.name(context).lowercase()
        runCatching { d?.collection("tokens")?.document(id)?.delete()?.await() }
        if (phone.isNotBlank()) runCatching { d?.collection("users")?.document(phone)?.delete()?.await() }
        if (nick.isNotBlank()) runCatching { d?.collection("nicknames")?.document(nick)?.delete()?.await() }
        // pulizia stato locale (id, profilo, foto, flag)
        Profile.clearAll(context)
    }

    /** Registra il mio numero nel registro "users" (chi ha l'app). Idempotente. */
    suspend fun registerMe(phone: String, id: String, name: String) {
        if (phone.isBlank()) return
        runCatching {
            db?.collection("users")?.document(phone)?.set(
                mapOf("phone" to phone, "id" to id, "name" to name,
                      "updatedAt" to System.currentTimeMillis())
            )?.await()
        }
    }

    /**
     * Registrazione UNA-TANTUM: nickname univoco + telefono.
     * Usa una transazione per garantire che due utenti non prendano lo stesso nickname.
     * Lo stesso telefono può "riprendersi" il proprio nickname (es. dopo reinstallazione).
     */
    suspend fun registerOnce(rawNickname: String, rawPhone: String, id: String): RegisterResult {
        val d = db ?: return RegisterResult.Error
        val phone = Phones.normalizeIt(rawPhone) ?: return RegisterResult.InvalidPhone
        val nick = rawNickname.trim()
        if (nick.length < 3 || nick.length > 20 ||
            !nick.all { it.isLetterOrDigit() || it == '.' || it == '_' || it == '-' }) {
            return RegisterResult.InvalidNickname
        }
        val key = nick.lowercase()
        return try {
            d.runTransaction { tx ->
                val nickRef = d.collection("nicknames").document(key)
                val snap = tx.get(nickRef)
                if (snap.exists() && snap.getString("id") != id && snap.getString("phone") != phone) {
                    throw NicknameTakenException()
                }
                val now = System.currentTimeMillis()
                tx.set(nickRef, mapOf("nickname" to nick, "id" to id, "phone" to phone, "createdAt" to now))
                tx.set(
                    d.collection("users").document(phone),
                    mapOf("phone" to phone, "id" to id, "name" to nick, "nickname" to nick, "updatedAt" to now)
                )
                true
            }.await()
            RegisterResult.Success(nick, phone)
        } catch (e: NicknameTakenException) {
            RegisterResult.NicknameTaken
        } catch (e: Exception) {
            if (e.cause is NicknameTakenException) RegisterResult.NicknameTaken else RegisterResult.Error
        }
    }

    /** Dato un elenco di numeri E.164, restituisce quelli registrati (numero -> utente). */
    suspend fun findRegistered(phones: List<String>): Map<String, AppUser> {
        val d = db ?: return emptyMap()
        if (phones.isEmpty()) return emptyMap()
        val out = mutableMapOf<String, AppUser>()
        phones.distinct().chunked(10).forEach { chunk ->
            runCatching {
                val snap = d.collection("users").whereIn("phone", chunk).get().await()
                snap.documents.forEach { doc ->
                    val ph = doc.getString("phone") ?: return@forEach
                    out[ph] = AppUser(doc.getString("id") ?: "", doc.getString("name") ?: "", ph)
                }
            }
        }
        return out
    }

    /** Invita in-app un utente registrato all'evento: aggiunge il suo id agli invitati. */
    suspend fun inviteUserToEvent(eventId: String, userId: String) {
        if (userId.isBlank()) return
        runCatching {
            db?.collection("events")?.document(eventId)
                ?.update("invitedIds", FieldValue.arrayUnion(userId))?.await()
        }
    }

    suspend fun sendInAppInvite(eventId: String, phone: String) {
        runCatching {
            db?.collection("events")?.document(eventId)
                ?.collection("invites")?.document(phone)
                ?.set(mapOf("phone" to phone, "invitedAt" to System.currentTimeMillis()))?.await()
        }
    }

    suspend fun findRegisteredPhones(phones: List<String>): Set<String> {
        val d = db ?: return emptySet()
        if (phones.isEmpty()) return emptySet()
        val found = mutableSetOf<String>()
        phones.distinct().chunked(10).forEach { chunk ->
            runCatching {
                val snap = d.collection("users").whereIn("phone", chunk).get().await()
                snap.documents.forEach { doc -> doc.getString("phone")?.let { found.add(it) } }
            }
        }
        return found
    }

    private var friendshipReg: ListenerRegistration? = null
    fun openFriendshipEvents(): Flow<List<CoffeeEvent>> {
        if (friendshipReg == null) {
            runCatching {
                friendshipReg = db?.collection("events")
                    ?.whereEqualTo("mode", "AMICIZIA")
                    ?.addSnapshotListener { snap, _ ->
                        runCatching {
                            snap?.documents?.mapNotNull { it.toObject<CoffeeEvent>() }?.forEach { putEvent(it) }
                        }
                    }
            }
        }
        return eventsState.map { m ->
            val now = System.currentTimeMillis()
            m.values.filter { it.mode == "AMICIZIA" && !it.cancelled && it.remainingMillis(now) > 0 }
                .sortedByDescending { it.createdAt }
        }
    }

    private var incomingReg: ListenerRegistration? = null
    /** "Hai ricevuto un Extreme Coffee": eventi lanciati da ALTRI, ancora attivi. */
    fun incomingInvites(myId: String): Flow<List<CoffeeEvent>> {
        if (incomingReg == null) {
            runCatching {
                incomingReg = db?.collection("events")
                    ?.whereArrayContains("invitedIds", myId)
                    ?.addSnapshotListener { snap, _ ->
                        runCatching {
                            val list = snap?.documents?.mapNotNull { it.toObject<CoffeeEvent>() }
                            if (list != null) {
                                invitedEventsState.value = list
                                list.forEach { putEvent(it) }   // così InvitePopup/Tracking lo trovano via eventFlow
                            }
                        }
                    }
            }
        }
        return combine(invitedEventsState, declinedState) { list, declined ->
            val now = System.currentTimeMillis()
            list.filter { it.launcherId != myId && !it.cancelled && it.remainingMillis(now) > 0 && it.id !in declined }
                .sortedByDescending { it.createdAt }
        }
    }

    private val eventRegs = mutableSetOf<String>()
    /** L'Extreme Coffee attivo lanciato da me (se c'è), altrimenti null. */
    fun myActiveEvent(myId: String): Flow<CoffeeEvent?> = eventsState.map { m ->
        val now = System.currentTimeMillis()
        m.values.filter { it.launcherId == myId && !it.cancelled && it.remainingMillis(now) > 0 }
            .maxByOrNull { it.createdAt }
    }

    fun eventFlow(eventId: String): Flow<CoffeeEvent?> {
        if (eventRegs.add(eventId)) {
            runCatching {
                db?.collection("events")?.document(eventId)
                    ?.addSnapshotListener { snap, _ ->
                        runCatching { snap?.toObject<CoffeeEvent>()?.let { putEvent(it) } }
                    }
            }
        }
        return eventsState.map { it[eventId] }
    }

    private val locRegs = mutableSetOf<String>()
    fun participantLocations(eventId: String): Flow<List<ParticipantLocation>> {
        if (locRegs.add(eventId)) {
            runCatching {
                db?.collection("events")?.document(eventId)?.collection("locations")
                    ?.addSnapshotListener { snap, _ ->
                        runCatching {
                            val list = snap?.documents?.mapNotNull { it.toObject<ParticipantLocation>() }
                            if (list != null) putLocations(eventId, list)
                        }
                    }
            }
        }
        return locationsState.map { it[eventId].orEmpty() }
    }
}

/** Eccezione interna usata per annullare la transazione quando il nickname è già preso. */
private class NicknameTakenException : Exception()

/** Esito della registrazione una-tantum. */
sealed class RegisterResult {
    data class Success(val nickname: String, val phone: String) : RegisterResult()
    object NicknameTaken : RegisterResult()
    object InvalidNickname : RegisterResult()
    object InvalidPhone : RegisterResult()
    object Error : RegisterResult()
}
