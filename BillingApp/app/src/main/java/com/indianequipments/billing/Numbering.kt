package com.indianequipments.billing

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object Numbering {
    private const val PREF = "billing_numbering"
    fun financialYear(): String {
        val c=Calendar.getInstance(); val y=c.get(Calendar.YEAR); val m=c.get(Calendar.MONTH)+1
        val start=if(m>=4)y else y-1
        return "%d-%02d".format(Locale.US,start,(start+1)%100)
    }
    fun next(context: Context): String {
        val p=context.getSharedPreferences(PREF,Context.MODE_PRIVATE)
        val fy=financialYear(); val lastFy=p.getString("fy",""); val seq=if(lastFy==fy)p.getInt("seq",0)+1 else 1
        p.edit().putString("fy",fy).putInt("seq",seq).apply()
        return "IE/$fy/$seq"
    }
    fun date(): String = SimpleDateFormat("dd/MM/yyyy",Locale.US).format(java.util.Date())
}
