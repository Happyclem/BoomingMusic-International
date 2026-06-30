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

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.mardous.booming.R
import com.mardous.booming.databinding.FragmentSettingsSearchBinding
import com.mardous.booming.extensions.applyHorizontalWindowInsets
import com.mardous.booming.extensions.hideSoftKeyboard
import com.mardous.booming.extensions.resources.focusAndShowKeyboard

/**
 * An Android Settings–style search screen that lets the user type a query and jump straight to the
 * matching preference, wherever it lives across the settings sub-screens.
 */
class SettingsSearchFragment : Fragment(R.layout.fragment_settings_search) {

    private var _binding: FragmentSettingsSearchBinding? = null
    private val binding get() = _binding!!

    private lateinit var searchIndex: SettingsSearchIndex
    private lateinit var searchAdapter: SettingsSearchAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSettingsSearchBinding.bind(view)
        view.applyHorizontalWindowInsets()

        searchIndex = SettingsSearchIndex(requireContext())
        searchAdapter = SettingsSearchAdapter { entry -> openEntry(entry) }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = searchAdapter
        }

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        binding.clearText.setOnClickListener {
            binding.searchView.text?.clear()
        }
        binding.searchView.apply {
            doAfterTextChanged { editable ->
                val query = editable?.toString()
                binding.clearText.isVisible = !query.isNullOrEmpty()
                search(query)
            }
            focusAndShowKeyboard()
        }
    }

    private fun search(query: String?) {
        val results = searchIndex.search(query)
        searchAdapter.submitList(results)
        binding.empty.isVisible = results.isEmpty() && !query.isNullOrBlank()
    }

    private fun openEntry(entry: SettingsSearchEntry) {
        activity?.hideSoftKeyboard()
        findNavController().navigate(
            entry.screen.searchNavAction,
            Bundle().apply { putString(ARG_HIGHLIGHT_KEY, entry.key) }
        )
    }

    override fun onDestroyView() {
        activity?.hideSoftKeyboard()
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_HIGHLIGHT_KEY = "highlight_key"
    }
}
