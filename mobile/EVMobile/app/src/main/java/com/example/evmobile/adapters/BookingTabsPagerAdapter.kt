package com.example.evmobile.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.evmobile.fragments.PastHistoryFragment
import com.example.evmobile.fragments.UpcomingBookingsFragment

class BookingTabsPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
    
    companion object {
        const val TAB_COUNT = 2
        const val TAB_UPCOMING_BOOKINGS = 0
        const val TAB_PAST_HISTORY = 1
    }
    
    override fun getItemCount(): Int = TAB_COUNT
    
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            TAB_UPCOMING_BOOKINGS -> UpcomingBookingsFragment()
            TAB_PAST_HISTORY -> PastHistoryFragment()
            else -> throw IllegalArgumentException("Invalid tab position: $position")
        }
    }
    
    fun getTabTitle(position: Int): String {
        return when (position) {
            TAB_UPCOMING_BOOKINGS -> "Upcoming Bookings"
            TAB_PAST_HISTORY -> "Past History"
            else -> ""
        }
    }
}