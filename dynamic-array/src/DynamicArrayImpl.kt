import kotlinx.atomicfu.*
import java.util.concurrent.atomic.AtomicMarkableReference
import java.util.concurrent.atomic.AtomicReferenceArray

class DynamicArrayImpl<E> : DynamicArray<E> {
    private val core = atomic(Core<E>(capacity = INITIAL_CAPACITY))

    override fun get(index: Int): E {
        val core = core.value
        core.completeOp()

        if (index >= core.size)
            throw IllegalArgumentException()

        return core.get(index)
    }

    override fun put(index: Int, element: E) {
        while (true) {
            val oldCore = core.value
            oldCore.completeOp()

            if (index >= oldCore.size)
                throw IllegalArgumentException()

            val newCore = Core.copy(oldCore.move(), op = AtomicMarkableReference({
                oldCore.array.set(index, element)
            }, false))
            if (core.compareAndSet(oldCore, newCore)) {
                newCore.completeOp()
                break
            }
        }
    }

    override fun pushBack(element: E) {
        while (true) {
            val oldCore = core.value
            oldCore.completeOp()

            val newCore = Core.createForPush(oldCore.move(), element)
            if (core.compareAndSet(oldCore, newCore)) {
                newCore.completeOp()
                break
            }
        }
    }

    override val size: Int
        get() {
            val core = core.value
            val size = core.size

            return if (core.op.isMarked) size - 1 else size
        }
}


private class Core<E>(
    val size: Int = 0,
    val capacity: Int,
    val ltm: Int = 0,
    val op: AtomicMarkableReference<(() -> Unit)?> = AtomicMarkableReference(null, false),
    val array: AtomicReferenceArray<E?> = AtomicReferenceArray(capacity),
    val oldArray: AtomicReferenceArray<E?> = AtomicReferenceArray(0),
) {
    private val moved: Int
        get() = capacity / 2 - ltm

    fun get(index: Int): E {
        return array.get(index) ?: oldArray.get(index)!!
    }

    fun completeOp() {
        val op1 = op.reference ?: return

        try {
            op1.invoke()
        } finally {
            op.set(null, false)
        }
    }

    fun move(): Core<E> {
        if (ltm == 0) {
            return this
        }

        array.compareAndSet(moved, null, oldArray.get(moved))
        return copy(this, ltm = ltm - 1)
    }

    companion object {
        fun <E> copy(
            other: Core<E>,
            size: Int = other.size,
            capacity: Int = other.capacity,
            ltm: Int = other.ltm,
            op: AtomicMarkableReference<(() -> Unit)?> = other.op,
            array: AtomicReferenceArray<E?> = other.array,
            oldArray: AtomicReferenceArray<E?> = other.oldArray,
        ): Core<E> {
            return Core(size, capacity, ltm, op, array, oldArray)
        }

        fun <E> createForPush(oldCore: Core<E>, e: E): Core<E> {
            val array = if (oldCore.capacity == oldCore.size) {
                AtomicReferenceArray(oldCore.capacity * 2)
            } else {
                oldCore.array
            }
            val op: AtomicMarkableReference<(() -> Unit)?> = AtomicMarkableReference({
                array.compareAndSet(oldCore.size, null, e)
            }, true)

            return if (oldCore.capacity == oldCore.size) {
                Core(
                    size = oldCore.size + 1,
                    capacity = oldCore.capacity * 2,
                    ltm = oldCore.capacity,
                    op = op,
                    array = array,
                    oldArray = oldCore.array,
                )
            } else {
                copy(
                    oldCore,
                    size = oldCore.size + 1,
                    op = op
                )
            }
        }
    }
}

private const val INITIAL_CAPACITY = 1 // DO NOT CHANGE ME