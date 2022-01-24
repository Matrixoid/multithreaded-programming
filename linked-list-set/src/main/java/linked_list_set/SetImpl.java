package linked_list_set;

import kotlinx.atomicfu.AtomicRef;

public class SetImpl implements Set {

    private interface AtomicMarkableReference {}

    private class RemovedFlag implements AtomicMarkableReference {
        Node node;
        private RemovedFlag(Node otherNode) {
            this.node = otherNode;
        }
    }

    private class Node implements  AtomicMarkableReference {
        AtomicRef<AtomicMarkableReference> next;
        int x;

        Node(int x, Node next) {
            this.next = new AtomicRef<AtomicMarkableReference>(next);
            this.x = x;
        }
    }

    private class Window {
        Node cur, next;
    }

    private final Node head = new Node(Integer.MIN_VALUE, new Node(Integer.MAX_VALUE, null));

    /**
     * Returns the {@link Window}, where cur.x < x <= next.x
     */
    private Window findWindow(int x) {
        retry:
        while(true) {
            Window w = new Window();
            w.cur = head;
            w.next = (Node) w.cur.next.getValue();
            while (w.next.x < x) {
                AtomicMarkableReference node = w.next.next.getValue();
                if(node instanceof RemovedFlag) {
                    if(!(w.cur.next.compareAndSet(w.next, ((RemovedFlag) node).node)))
                        continue retry;
                    w.next = ((RemovedFlag) node).node;
                } else {
                    w.cur = w.next;
                    w.next = (Node) node;
                }
            }

            return w;

        }
    }

    @Override
    public boolean add(int x) {
        while (true) {
            Window w = findWindow(x);
            boolean res;
            if ((w.next.x == x) && (w.next.next.getValue() instanceof Node)) {
                return false;
            } else {
                if(w.cur.next.compareAndSet(w.next, new Node (x, w.next)))
                    return true;
            }
        }
    }

    @Override
    public boolean remove(int x) {
        while(true) {
            Window w = findWindow(x);
            AtomicMarkableReference node = w.next.next.getValue();
            boolean res;
            if (w.next.x != x || node instanceof RemovedFlag) {
                return false;
            } else {
                //AtomicMarkableReference n = w.next.next.getValue();
                if(w.next.next.compareAndSet(node, new RemovedFlag((Node) node))) {
                    w.cur.next.compareAndSet(w.next, node);
                    return true;
                }
            }
        }
    }

    @Override
    public boolean contains(int x) {
        Window w = findWindow(x);
        boolean res = (w.next.x == x) && (w.next.next.getValue() instanceof Node);
        return res;
    }
}