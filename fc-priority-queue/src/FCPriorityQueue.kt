import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.atomicArrayOfNulls
import java.util.*
import kotlin.random.Random

class FCPriorityQueue<E : Comparable<E>> {
    private class Add<E>(val element: E)
    private class Poll
    private class Peek
    private class Result(val result: Any?)

    private val queue = PriorityQueue<E>()
    private val flatCombiningcArray = atomicArrayOfNulls<Any>(FC_ARRAY_SIZE)
    private val flatCombiningcLock = atomic(false)

    private fun combine() {
        for (index in 0 until FC_ARRAY_SIZE) {
            when (val status = flatCombiningcArray[index].value) {
                is Add<*> -> { @Suppress("UNCHECKED_CAST") queue.add(status.element as E); flatCombiningcArray[index].value = Result(Unit)}
                is Poll -> flatCombiningcArray[index].value = Result(queue.poll())
                is Peek -> flatCombiningcArray[index].value = Result(queue.peek())
            }
        }
    }

    private fun <T> common(exe: () -> T, op: Any): T {
        if (flatCombiningcLock.compareAndSet(expect = false, update = true)) {
            val result = exe(); combine()
            return result.also { flatCombiningcLock.compareAndSet(expect = true, update = false) }
        }

        var index = Random.nextInt(FC_ARRAY_SIZE)
        while (!flatCombiningcArray[index].compareAndSet(null, op)) {
            index = (index + 1) % FC_ARRAY_SIZE
        }

        while (true) {
            val status = flatCombiningcArray[index].value
            if (status is Result) {
                val result = status.result
                flatCombiningcArray[index].compareAndSet(status, null)
                @Suppress("UNCHECKED_CAST")
                return result as T
            }

            if (flatCombiningcLock.compareAndSet(expect = false, update = true)) {
                @Suppress("NAME_SHADOWING") val status = flatCombiningcArray[index].value
                @Suppress("UNCHECKED_CAST") val result = if (status is Result) status.result as T else exe()
                flatCombiningcArray[index].value = null; combine()
                return result.also { flatCombiningcLock.compareAndSet(expect = true, update = false) }
            }
        }
    }

    /**
     * Retrieves the element with the highest priority
     * and returns it as the result of this function;
     * returns `null` if the queue is empty.
     */
    fun poll(): E? {
        return common<E?>({ queue.poll() }, Poll())
    }

    /**
     * Returns the element with the highest priority
     * or `null` if the queue is empty.
     */
    fun peek(): E? {
        return common<E?>({ queue.peek() }, Peek())
    }

    /**
     * Adds the specified element to the queue.
     */
    fun add(element: E) {
        return common(fun() { queue.add(element) }, Add(element))
    }
}

const val FC_ARRAY_SIZE = 16