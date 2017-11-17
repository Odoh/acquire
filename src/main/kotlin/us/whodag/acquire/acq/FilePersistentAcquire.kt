package us.whodag.acquire.acq

import us.whodag.acquire.req.AcqReq
import us.whodag.acquire.Acquire
import us.whodag.acquire.req.Response
import us.whodag.acquire.req.isFailure
import us.whodag.acquire.json.StandardAcquirePersist
import us.whodag.acquire.json.json
import java.io.File

/**
 * An Acquire game which is persisted to a file.
 */
class FilePersistentAcquire(val acq: Acquire, val path: String) : Acquire by acq {
    override fun submit(req: AcqReq): Response {
        val response = acq.submit(req)
        if (response.isFailure()) return response

        // save acquire into a file
        val persist = StandardAcquirePersist(acq)
        val file = File(path)
        file.parentFile.mkdirs()
        file.printWriter().use { out -> out.print(persist.json()) }
        return response
    }
}
