/**
 * @author :TODO: Chmykhalov Artemiy
 */
class Solution : AtomicCounter {
    private val root : Node = Node(0)
    private val last : ThreadLocal<Node> = ThreadLocal.withInitial { root }

    override fun getAndAdd(x: Int): Int {
        while (true) {
            val curNode : Node = last.get()
            val curValue : Int = curNode.value
            val newNode : Node = Node(curValue + x)

            val next : Node = curNode.next.decide(newNode)
            last.set(next)

            if (next == newNode) {
                return curValue
            }
        }
    }

    // вам наверняка потребуется дополнительный класс
    private class Node(val value: Int) {
        val next : Consensus<Node> = Consensus()
    }
}
