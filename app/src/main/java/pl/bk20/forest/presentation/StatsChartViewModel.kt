package pl.bk20.forest.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import pl.bk20.forest.ForestApplication
import pl.bk20.forest.data.repository.DayRepositoryImpl
import pl.bk20.forest.domain.model.Day
import pl.bk20.forest.domain.usecase.StatsUseCases
import pl.bk20.forest.util.iterator
import java.time.LocalDate

class StatsChartViewModel(
    private val statsUseCases: StatsUseCases
) : ViewModel() {

    private val _week = MutableStateFlow<List<Day>>(emptyList())
    val week: StateFlow<List<Day>> = _week.asStateFlow()

    private var getWeekJob: Job? = null

    fun selectWeek(firstDay: LocalDate) {
        getWeekJob?.cancel()
        getWeekJob = statsUseCases.getWeek(firstDay).onEach {
            _week.value = alignWeek(it, firstDay)
        }.launchIn(viewModelScope)
    }

    private fun alignWeek(
        week: List<Day>,
        firstDay: LocalDate,
        lastDay: LocalDate = firstDay.plusDays(6)
    ): List<Day> {
        val result = week.toMutableList()
        for (date in firstDay..lastDay) {
            val shouldAddDay = result.none { it.date == date }
            if (shouldAddDay) {
                result.add(Day(date, goal = 0))
            }
        }
        return result.sortedBy { it.date }
    }

    companion object Factory : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val application = checkNotNull(extras[APPLICATION_KEY]) as ForestApplication

            val forestDatabase = application.forestDatabase
            val dayRepository = DayRepositoryImpl(forestDatabase.dayDao)
            val statsUseCases = StatsUseCases(dayRepository)

            return StatsChartViewModel(statsUseCases) as T
        }
    }
}