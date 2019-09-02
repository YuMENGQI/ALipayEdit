package com.example.alipayedit

import android.app.Activity
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.annotation.Nullable
import androidx.recyclerview.widget.GridLayoutManager
import java.util.ArrayList
import androidx.recyclerview.widget.RecyclerView as RecyclerView1

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var recyclerViewExist: RecyclerView1
    private lateinit var recyclerViewAll: RecyclerView1

    private lateinit var horizonLScrollView: HorizontalScrollView
    private var rg_tab: RadioGroup? = null

    private lateinit var blockAdapter: FunctionBlockAdapter
    private lateinit var functionAdapter: FunctionAdapter
    private lateinit var gridManager: GridLayoutManager

    private val scrollTab = ArrayList<String>()

    private var itemWidth = 0
    private var lastRow = 0
    private var isMove = false//滑动状态
    private var scrollPosition = 0
    private var currentTab: String? = null//当前的标签
    private var tabWidth = 0//标签宽度


    private var allData: List<FunctionItem>? = null
    private var selData: MutableList<FunctionItem>? = null
    private var sfUtils: SFUtils? = null
    private val MAX_COUNT = 14
    private var isDrag = false

    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        init()
        addListener()
    }


    fun init() {
        supportActionBar?.hide()
        recyclerViewExist = findViewById(R.id.recyclerViewExist)
        horizonLScrollView = findViewById(R.id.horizonLScrollView)
        rg_tab = findViewById(R.id.rg_tab)
        recyclerViewAll = findViewById(R.id.recyclerViewAll)
        sfUtils = SFUtils(this)
        allData = sfUtils!!.allFunctionWithState
        selData = sfUtils!!.selectFunctionItem

        blockAdapter = FunctionBlockAdapter(this, selData!!)
        recyclerViewExist.layoutManager = GridLayoutManager(this, 4)
        recyclerViewExist.adapter = blockAdapter
        recyclerViewExist.addItemDecoration(SpaceItemDecoration(4, dip2px(this, 10f)))

        val callback = DefaultItemCallback(blockAdapter)
        val helper = DefaultItemTouchHelper(callback)
        helper.attachToRecyclerView(recyclerViewExist)

        gridManager = GridLayoutManager(this, 4)
        gridManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                val fi = allData!![position]
                return if (fi.isTitle) 4 else 1
            }
        }

        functionAdapter = FunctionAdapter(this, allData!!)
        recyclerViewAll.layoutManager = gridManager
        recyclerViewAll.adapter = functionAdapter
        val spaceDecoration = SpaceItemDecoration(4, dip2px(this, 10f))
        recyclerViewAll.addItemDecoration(spaceDecoration)

        itemWidth = getAtyWidth(this) / 4 + dip2px(this, 2f)

        resetEditHeight(selData!!.size)

        initTab()
    }


    private fun getAtyWidth(context: Context): Int {
        return try {
            val mDm = DisplayMetrics()
            (context as Activity).windowManager.defaultDisplay
                .getMetrics(mDm)
            mDm.widthPixels
        } catch (e: Exception) {
            0
        }

    }

    private fun addListener() {
        findViewById<View>(R.id.submit).setOnClickListener {
            sfUtils!!.saveSelectFunctionItem(selData)
            sfUtils!!.saveAllFunctionWithState(allData)
        }
        functionAdapter.setOnItemAddListener(FunctionAdapter.OnItemAddListener { item ->
            if (selData != null && selData!!.size < MAX_COUNT) {   // 更新选择列表，所有列表已在内部进行更新
                try {
                    selData!!.add(item)
                    resetEditHeight(selData!!.size)
                    blockAdapter.notifyDataSetChanged()
                    item.isSelect = true
                    return@OnItemAddListener true
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                false
            } else {
                Toast.makeText(this@MainActivity, "选中的模块不能超过" + MAX_COUNT + "个", Toast.LENGTH_SHORT)
                    .show()
                false
            }
        })

        blockAdapter.setOnItemRemoveListener { item ->
            // 更新所有列表，选择列表已在内部进行更新
            try {
                if (item?.name != null) {
                    for (i in allData!!.indices) {
                        val data = allData!![i]
                        if (data.name != null) {
                            if (item.name == data.name) {
                                data.isSelect = false
                                break
                            }
                        }
                    }
                    functionAdapter.notifyDataSetChanged()
                }
                resetEditHeight(selData!!.size)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        recyclerViewAll.addOnScrollListener(onScrollListener)
    }

    override fun onClick(v: View) {}

    private fun initTab() {
        try {
            val tabs = sfUtils!!.tabNames


            if (tabs != null && tabs.size > 0) {
                currentTab = tabs[0].name
                val padding = dip2px(this, 10f)
                val size = tabs.size
                for (i in 0 until size) {
                    val item = tabs[i]
                    if (item.isTitle) {
                        scrollTab.add(item.name)
                        val rb = RadioButton(this)
                        rb.setPadding(padding, 0, padding, 0)
                        rb.buttonDrawable = null
                        rb.gravity = Gravity.CENTER
                        rb.text = item.name
                        rb.tag = item.subItemCount
                        rb.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                        try {
                            rb.setTextColor(resources.getColorStateList(R.color.bg_block_text))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                        rb.setCompoundDrawablesWithIntrinsicBounds(
                            null,
                            null,
                            null,
                            resources.getDrawable(R.drawable.bg_block_tab)
                        )
                        rb.setOnCheckedChangeListener(onCheckedChangeListener)
                        rg_tab!!.addView(rb)
                    }
                }
                (rg_tab!!.getChildAt(0) as RadioButton).isChecked = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private val onCheckedChangeListener =
        CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
            try {
                val position = buttonView.tag as Int
                val text = buttonView.text.toString()
                if (currentTab != text && isChecked) {
                    currentTab = text
                    moveToPosition(position)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    private fun resetEditHeight(size: Int) {
        var size = size
        try {
            if (size == 0) {
                size = 1
            }
            var row = size / 4 + if (size % 4 > 0) 1 else 0
            if (row <= 0)
                row = 1
            if (lastRow != row) {
                lastRow = row
                val params = recyclerViewExist.layoutParams
                params.height = itemWidth * row
                recyclerViewExist.layoutParams = params
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun moveToPosition(position: Int) {
        val first = gridManager.findFirstVisibleItemPosition()
        val end = gridManager.findLastVisibleItemPosition()
        if (first == -1 || end == -1)
            return
        if (position <= first) {      //移动到前面
            gridManager.scrollToPosition(position)
        } else if (position >= end) {      //移动到后面
            isMove = true
            scrollPosition = position
            gridManager.smoothScrollToPosition(recyclerViewAll, null, position)
        } else {//中间部分
            val n = position - gridManager.findFirstVisibleItemPosition()
            if (n > 0 && n < allData!!.size) {
                val top = gridManager.findViewByPosition(position)!!.top
                recyclerViewAll.scrollBy(0, top)
            }
        }
    }

    private val onScrollListener = object : RecyclerView1.OnScrollListener() {

        override fun onScrollStateChanged(recyclerView: RecyclerView1, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            try {
                if (isMove && newState == RecyclerView1.SCROLL_STATE_IDLE) {
                    isMove = false
                    val view = gridManager.findViewByPosition(scrollPosition)
                    if (view != null) {
                        val top = view.top
                        recyclerView.scrollBy(0, top)
                    }
                }
                isDrag = newState == RecyclerView1.SCROLL_STATE_DRAGGING
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }

        override fun onScrolled(recyclerView: RecyclerView1, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            if (isDrag) {  //拖动过程中
                val position = gridManager.findFirstVisibleItemPosition()
                if (position > 0) {
                    for (i in 0 until position + 1) {
                        if (allData!![i].isTitle) {
                            currentTab = allData!![i].name
                        }
                    }
                    scrollTab(currentTab)
                }
            }
        }
    }


    private fun scrollTab(newTab: String?) {
        try {
            val position = scrollTab.indexOf(currentTab)
            val targetPosition = scrollTab.indexOf(newTab)
            currentTab = newTab
            if (targetPosition != -1) {
                val x = (targetPosition - position) * getTabWidth()
                val radioButton = rg_tab!!.getChildAt(targetPosition) as RadioButton
                radioButton.setOnCheckedChangeListener(null)
                radioButton.isChecked = true
                radioButton.setOnCheckedChangeListener(onCheckedChangeListener)
                horizonLScrollView.scrollBy(x, 0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun getTabWidth(): Int {
        if (tabWidth == 0) {
            if (rg_tab != null && rg_tab!!.childCount != 0) {
                tabWidth = rg_tab!!.width / rg_tab!!.childCount
            }
        }
        return tabWidth
    }

    private fun dip2px(context: Context, dpValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }
}

