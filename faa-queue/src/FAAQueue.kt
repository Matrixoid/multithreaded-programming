import kotlinx.atomicfu.*

class FAAQueue<T> {
    private val head: AtomicRef<Segment> // Head pointer, similarly to the Michael-Scott queue (but the first node is _not_ sentinel)
    private val tail: AtomicRef<Segment> // Tail pointer, similarly to the Michael-Scott queue

    init {
        val firstNode = Segment()
        head = atomic(firstNode)
        tail = atomic(firstNode)
    }

    /**
     * Adds the specified element [x] to the queue.
     */
    fun enqueue(x: T) {
        while (true) {
            val tail = tail.value
            val enqIdx = tail.enqIdx.getAndAdd(1)

            if (enqIdx >= SEGMENT_SIZE) {
                val newTail = Segment(x)
                if (tail.next.compareAndSet(null, newTail)) {
                    this.tail.compareAndSet(tail, newTail)
                    break
                } else {
                    tail.next.value?.let { this.tail.compareAndSet(tail, it) }
                    continue
                }
            } else if (tail.elements[enqIdx].compareAndSet(null, x)) {
                break
            }
        }
    }

    /**
     * Retrieves the first element from the queue
     * and returns it; returns `null` if the queue
     * is empty.
     */
    fun dequeue(): T? {
        while (true) {
            val head = head.value
            val deqIdx = head.deqIdx.getAndAdd(1)

            if (deqIdx >= SEGMENT_SIZE) {
                val nextHead = head.next.value ?: return null
                this.head.compareAndSet(head, nextHead)
            } else {
                return head.elements[deqIdx].getAndSet(DONE) as T? ?: continue
            }
        }
    }

    /**
     * Returns `true` if this queue is empty;
     * `false` otherwise.
     */
    val isEmpty: Boolean
        get() {
            while (true) {
                val head = head.value
                val nextHead = head.next.value
                val enqIdx = head.enqIdx.value
                val deqIdx = head.deqIdx.value

                if (deqIdx >= enqIdx || deqIdx >= SEGMENT_SIZE) {
                    if (nextHead == null) {
                        return true
                    } else {
                        this.head.compareAndSet(head, nextHead)
                    }
                } else {
                    return false
                }
            }
        }
}

private class Segment {
    val next: AtomicRef<Segment?> = atomic(null)
    val enqIdx: AtomicInt // index for the next enqueue operation
    val deqIdx = atomic(0) // index for the next dequeue operation
    val elements = atomicArrayOfNulls<Any>(SEGMENT_SIZE)

    constructor() { // for the first segment creation
        enqIdx = atomic(0) // index for the next enqueue operation
    }

    constructor(x: Any?) { // each next new segment should be constructed with an element
        enqIdx = atomic(1)
        elements[0].getAndSet(x)
    }
}

private val DONE = Any() // Marker for the "DONE" slot state; to avoid memory leaks
const val SEGMENT_SIZE = 2 // DO NOT CHANGE, IMPORTANT FOR TESTS