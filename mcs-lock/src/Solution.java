import java.util.concurrent.atomic.*;

public class Solution implements Lock<Solution.Node> {
    private final Environment env;
    private final AtomicReference<Node> tail = new AtomicReference<Node>(null);

    // todo: необходимые поля (final, используем AtomicReference)

    public Solution(Environment env) {
        this.env = env;
    }

    @Override
    public Node lock() {
        Node myNode = new Node(true); // сделали узел
        // todo: алгоритм
        Node prevNode = tail.getAndSet(myNode);
        if (prevNode != null) {
            prevNode.nextNode.set(myNode);
            while(myNode.nodeLock.get())
                env.park();
        }
        return myNode; // вернули узел
    }

    @Override
    public void unlock(Node node) {
        // todo: алгоритм

        if(node.nextNode.get() == null)
            if(tail.compareAndSet(node, null))
                return;
            else
                while(node.nextNode.get() == null) {}
        node.nextNode.get().nodeLock.set(false);
        env.unpark(node.nextNode.get().thread);
    }

    static class Node {

        public Node(boolean nodeLock) {
            this.nodeLock = new AtomicReference<Boolean>(nodeLock);
        }

        final Thread thread = Thread.currentThread(); // запоминаем поток, которые создал узел
        final AtomicReference<Node> nextNode = new AtomicReference<Node>(null);
        final AtomicReference<Boolean> nodeLock;
        // todo: необходимые поля (final, используем AtomicReference)
    }
}
