package uz.exemple.less64_tasks_kotlin.adapter

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import uz.exemple.less64_tasks_kotlin.R
import uz.exemple.less64_tasks_kotlin.model.ExternalPhotos

class ExternalAdapter(var context: Context,var items:List<Uri>):RecyclerView.Adapter<ExternalAdapter.ViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_photo,parent,false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val photo = items[position]

        holder.photo.setImageURI(photo)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        lateinit var photo:ImageView
        init {
            photo = itemView.findViewById<ImageView>(R.id.ivPhoto)
        }
    }
}