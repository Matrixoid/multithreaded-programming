package dijkstra

import kotlinx.atomicfu.AtomicBooleanArray
import java.util.*
import java.util.concurrent.Phaser
import java.util.concurrent.atomic.AtomicInteger
import kotlin.Comparator
import kotlin.concurrent.thread
import kotlin.random.Random

private val NODE_DISTANCE_COMPARATOR = Comparator<Node> { o1, o2 -> Integer.compare(o1!!.distance, o2!!.distance) }

class PMQ<E> (n: Int, comp: Comparator<E>) {

    private var queueSize = 2 * n
    private var compar = nullsLast(comp)
    private var q = Array(queueSize + 1) { PriorityQueue<E>(comp) }
    final val locks = AtomicBooleanArray(queueSize + 1)

    fun add(element: E) {
        while (true) {
            val index = Random.nextInt(queueSize)
            if (locks[index].compareAndSet(expect = false, update = true)) {
                q[index].offer(element)
                locks[index].value = false
                break
            }
        }
    }

    fun poll(): E? {

        while(true){
            var fstIndex = Random.nextInt(queueSize + 1)
            var sndIndex = Random.nextInt(queueSize)
            if(sndIndex == fstIndex) {
                sndIndex = sndIndex + 1
            }

            if(locks[fstIndex].compareAndSet(expect = false, update = true)) {
                if(locks[sndIndex].compareAndSet(expect = false, update = true)) {

                    var fstIndexv: E? = null
                    var sndIndexv: E? = null

                    if(q[fstIndex].isNotEmpty())
                        fstIndexv = q[fstIndex].peek()
                    if(q[sndIndex].isNotEmpty())
                        sndIndexv = q[sndIndex].peek()

                    locks[fstIndex].value = false
                    locks[sndIndex].value = false

                    if (compar.compare(fstIndexv, sndIndexv) < 0)
                        return q[fstIndex].poll()
                    else
                        return q[sndIndex].poll()
                }
            }
        }
    }

    fun updateDistanceIfLower(node: Node, newDistance: Int): Boolean {
        while (true) {
            val curDistance = node.distance
            if (curDistance <= newDistance) {
                return false
            }
            if (node.casDistance(curDistance, newDistance)) {
                return true
            }
        }
    }

}

// Returns `Integer.MAX_VALUE` if a path has not been found.
fun shortestPathParallel(start: Node) {
    val workers = Runtime.getRuntime().availableProcessors()
    // The distance to the start node is `0`
    start.distance = 0
    // Create a priority (by distance) queue and add the start node into it
    val q = PMQ(workers, NODE_DISTANCE_COMPARATOR) // TODO replace me with a multi-queue based PQ!
    q.add(start)
    // Run worker threads and wait until the total work is done
    val activeNodes = AtomicInteger(1)
    val onFinish = Phaser(workers + 1) // `arrive()` should be invoked at the end by each worker
    repeat(workers) {
        thread {
            while (activeNodes.get() > 0) {
                // TODO Write the required algorithm here,
                // TODO break from this loop when there is no more node to process.
                // TODO Be careful, "empty queue" != "all nodes are processed".
                val cur: Node = synchronized(q) { q.poll() } ?: continue
                val curDistance = cur.distance
                for (e in cur.outgoingEdges) {
                    val newDistance = curDistance + e.weight
                    if(q.updateDistanceIfLower(e.to, newDistance)) {
                        synchronized(q) {q.add(e.to)}
                        activeNodes.incrementAndGet()
                    }
//                    if (e.to.distance > cur.distance + e.weight) {
//                        e.to.distance = cur.distance + e.weight
//                        q.addOrDecreaseKey(e.to)
//                    }
                }
                activeNodes.decrementAndGet()
            }
            onFinish.arrive()
        }
    }
    onFinish.arriveAndAwaitAdvance()
}