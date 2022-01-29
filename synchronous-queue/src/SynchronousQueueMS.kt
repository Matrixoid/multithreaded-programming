import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SynchronousQueueMS<E> : SynchronousQueue<E> {
    private object RETRY

    inner class Node(val element: E?, val cont: Continuation<Any?>?) {
        val next = atomic<Node?>(null)
    }

    private val head: AtomicRef<Node>
    private val tail: AtomicRef<Node>

    init {
        val dummy = Node(null, null)
        head = atomic(dummy)
        tail = atomic(dummy)
    }

    private suspend fun enqueueAndSuspend(tail: Node, e: E?): Any? {
        return suspendCoroutine { cont ->
            val queueTail = Node(e, cont)
            var retry = true
            if (tail.next.compareAndSet(null, queueTail)) {
                retry = false
            } else {
                retry = true
            }
            this.tail.compareAndSet(tail, tail.next.value!!)

            if (retry) {
                cont.resume(RETRY)
            }
        }
    }

    private fun dequeueAndResume(head: Node, e: E?): Any? {
        val queueHead = head.next.value!!
        if (this.head.compareAndSet(head, queueHead)) {
            queueHead.cont!!.resume(e)
           return queueHead.element
        } else {
            return RETRY
        }
    }

    override suspend fun send(element: E) {
        while (true) {
            val head = head.value
            val tail = tail.value

            var result: Any? = null

            if (head == tail || tail.element != null) {
                result = enqueueAndSuspend(tail, element)
            } else {
                result = dequeueAndResume(head, element)
            }

            if (result != RETRY) {
                break
            }
        }
    }

    override suspend fun receive(): E {
        while (true) {
            val head = head.value
            val tail = tail.value

            var result: Any? = null

            if (head == tail || tail.element == null) {
                result = enqueueAndSuspend(tail, null)
            } else {
                result = dequeueAndResume(head, null)
            }

            if (result != RETRY) {
                @Suppress("UNCHECKED_CAST")
                return result as E
            }
        }
    }
}