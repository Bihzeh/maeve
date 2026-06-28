package gg.snell.mod.menu

/**
 * Pure data model for the bespoke multiplayer server picker. No Minecraft types, so the screen's
 * runtime layer can map vanilla `ServerData`/ping results onto these and the layout/renderer stay
 * headlessly testable.
 */

/** Reachability of a server entry, driving its row pill. */
enum class ServerStatus { Online, Offline, Pinging }

/**
 * One row in the server list.
 *
 * @param name    display label (server nickname)
 * @param address host[:port] the row connects to
 * @param motd    server "message of the day" / description line (may be empty)
 * @param players online/max as a pre-formatted string, e.g. "12/100" (empty when unknown)
 * @param ping    round-trip in milliseconds; -1 when unknown / not yet pinged
 * @param status  reachability, selecting the row's status pill
 */
data class ServerRow(
    val name: String,
    val address: String,
    val motd: String,
    val players: String,
    val ping: Int,
    val status: ServerStatus,
)
