import kotlin.time.hours

/**
 * В теле класса решения разрешено использовать только переменные делегированные в класс RegularInt.
 * Нельзя volatile, нельзя другие типы, нельзя блокировки, нельзя лазить в глобальные переменные.
 *
 * @author :TODO: Chmykhalov Artemiy
 */
class Solution : MonotonicClock {
    private var clock1hours by RegularInt(0)
    private var clock1minutes by RegularInt(0)
    private var clock1seconds by RegularInt(0)
    private var clock2hours by RegularInt(0)
    private var clock2minutes by RegularInt(0)
    private var clock2seconds by RegularInt(0)

    override fun write(time: Time) {
        clock2hours = time.d1
        clock2minutes = time.d2
        clock2seconds = time.d3
        // write right-to-left
        clock1seconds = clock2seconds
        clock1minutes = clock2minutes
        clock1hours = clock2hours
    }

    override fun read(): Time {
        val read1hours = clock1hours
        val read1minutes = clock1minutes
        val read1seconds = clock1seconds

        val read2seconds = clock2seconds
        val read2minutes = clock2minutes
        val read2hours = clock2hours

        if (read1hours ==  read2hours) {
            if(read1minutes == read2minutes) {
                return Time(read2hours, read2minutes, read2seconds)
            } else {
                return Time( read2hours, read2minutes, 0)
            }
        } else {
            return Time( read2hours, 0, 0)
        }

    }
}