package com.aysekayabay.recipeappsqlite

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ListFragment : Fragment() {
    var mealNameList = ArrayList<String>()
    var mealIdList = ArrayList<Int>()
    lateinit var recyclerView: RecyclerView
    private lateinit var  listAdapter : ListRecyclerAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listAdapter = ListRecyclerAdapter(mealNameList,mealIdList)
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = listAdapter
        getDataFromSql()
    }

    fun getDataFromSql(){
        try {
            activity?.let {
                val database = it.openOrCreateDatabase("Meals", Context.MODE_PRIVATE, null)
                val cursor = database.rawQuery("SELECT * FROM meals", null)
                val mealNameIndex = cursor.getColumnIndex("mealName")
                val mealIdIndex = cursor.getColumnIndex("id")

                mealNameList.clear()
                mealIdList.clear()

                while (cursor.moveToNext()){
                    mealNameList.add(cursor.getString(mealNameIndex))
                    mealIdList.add(cursor.getInt(mealIdIndex))
                }
                listAdapter.notifyDataSetChanged() // when data is changed it would be updated
                cursor.close()
            }

        }
        catch (e: Exception){
            e.printStackTrace()
        }
    }
}