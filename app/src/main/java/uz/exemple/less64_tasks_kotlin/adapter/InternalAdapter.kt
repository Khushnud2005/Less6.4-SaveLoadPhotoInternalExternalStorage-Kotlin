package uz.exemple.less64_tasks_kotlin.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import uz.exemple.less64_tasks_kotlin.R
import uz.exemple.less64_tasks_kotlin.model.InternalPhotos

class InternalAdapter(var context: Context, var photos:ArrayList<InternalPhotos>):RecyclerView.Adapter<InternalAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_photo,parent,false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val photo = photos[position]

        holder.photo.setImageBitmap(photo.bmp)
    }

    override fun getItemCount(): Int {
        return photos.size
    }
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        lateinit var photo:ImageView

        init {
            photo = itemView.findViewById<ImageView>(R.id.ivPhoto)
        }
    }

}