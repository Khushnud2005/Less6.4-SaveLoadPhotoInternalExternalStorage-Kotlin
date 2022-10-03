package uz.exemple.less64_tasks_kotlin.utils

import android.content.Context
import android.widget.Toast

object Utils {

    fun fireToast(context: Context,text:String){
        Toast.makeText(context,text,Toast.LENGTH_SHORT).show()
    }
}