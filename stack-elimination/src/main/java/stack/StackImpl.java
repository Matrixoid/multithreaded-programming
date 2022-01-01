package stack;

import kotlinx.atomicfu.AtomicArray;
import kotlinx.atomicfu.AtomicRef;

public class StackImpl implements Stack {

    public StackImpl() {
        for (int i = 0; i < 256; i++) {
            elimination_array.get(i).setValue(Integer.MIN_VALUE);
        }
    }

    private static class Node {
        final AtomicRef<Node> next;
        final int x;

        Node(int x, Node next) {
            this.next = new AtomicRef<>(next);
            this.x = x;
        }
    }

    // head pointer
    private AtomicRef<Node> head = new AtomicRef<>(null);
    private AtomicArray<Integer> elimination_array = new AtomicArray<>(256);

    @Override
    public void push(int x) {

        int idX = (int) (Math.random() * 256);
        boolean empty_place = true;

        for (int i = 0; i < 8; i++) {
            if (idX + i >= 256)
                break;
            if(elimination_array.get(idX + i).compareAndSet(Integer.MIN_VALUE, x)) {
                idX = idX + i;
                empty_place = false;
                break;
            }
        }

        if(!empty_place) {
            for(int i = 0; i < 8; i++) {
                if(elimination_array.get(idX).compareAndSet(Integer.MAX_VALUE, Integer.MIN_VALUE)) {
                    return;
                }
            }
        }

        while(true) {
            Node curHead = head.getValue();
            Node newNode = new Node(x, curHead);
            if (head.compareAndSet(curHead, newNode)) {
                return;
            }
        }
    }

    @Override
    public int pop() {

        int idX = (int) (Math.random() * 256);
        for (int i = 0; i < 8; i++) {
            if (idX + i >= 256)
                break;

            int val = elimination_array.get(idX + i).getValue();
            if(val == Integer.MIN_VALUE || val == Integer.MAX_VALUE) {
                continue;
            }
            if (elimination_array.get(idX).compareAndSet(val, Integer.MAX_VALUE)) {
                return val;
            }
        }

        while(true) {
            Node curHead = head.getValue();
            if (curHead == null) {
                return Integer.MIN_VALUE;
            }
            if (head.compareAndSet(curHead, curHead.next.getValue())) {
                return curHead.x;
            }
        }
    }
}
