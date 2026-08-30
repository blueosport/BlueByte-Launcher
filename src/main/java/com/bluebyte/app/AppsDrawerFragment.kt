package com.bluebyte.app

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bluebyte.app.databinding.FragmentAppsDrawerBinding

class AppsDrawerFragment : Fragment() {
    private var _binding: FragmentAppsDrawerBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: AppsDrawerAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAppsDrawerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = AppsDrawerAdapter(requireContext())
        binding.appDrawerRecylerView.layoutManager = LinearLayoutManager(context)
        binding.appDrawerRecylerView.adapter = adapter

        refreshFortune()

        binding.searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.filter(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    fun refreshFortune() {
        if (_binding != null) {
            binding.fortuneText.text = FortuneUtils.getFortune()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}