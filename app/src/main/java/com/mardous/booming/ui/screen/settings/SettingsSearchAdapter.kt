/*
 * Copyright (c) 2024 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mardous.booming.ui.screen.settings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import com.mardous.booming.R

class SettingsSearchAdapter(
    private val onClick: (SettingsSearchEntry) -> Unit
) : RecyclerView.Adapter<SettingsSearchAdapter.ViewHolder>() {

    private var dataSet: List<SettingsSearchEntry> = emptyList()

    fun submitList(entries: List<SettingsSearchEntry>) {
        dataSet = entries
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_settings_search_result, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = dataSet.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = dataSet[position]
        val context = holder.itemView.context
        holder.title.text = entry.title
        holder.breadcrumb.text = buildString {
            append(context.getString(entry.screen.titleRes))
            entry.categoryTitle?.let { append(" • ").append(it) }
        }
        holder.itemView.setOnClickListener { onClick(entry) }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: MaterialTextView = itemView.findViewById(R.id.title)
        val breadcrumb: MaterialTextView = itemView.findViewById(R.id.breadcrumb)
    }
}
