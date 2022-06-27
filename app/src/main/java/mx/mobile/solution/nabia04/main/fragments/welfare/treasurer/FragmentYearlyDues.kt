package mx.mobile.solution.nabia04.main.fragments.welfare.treasurer

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.amulyakhare.textdrawable.util.ColorGenerator
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.databinding.FragmentYearlyPaymentBinding
import mx.mobile.solution.nabia04.main.MainActivity.Companion.excelHelper
import mx.mobile.solution.nabia04.main.fragments.BaseDataBindingFragment
import mx.mobile.solution.nabia04.main.fragments.host_fragments.TreasurerPaymentDetailHost.Companion.SORT
import mx.mobile.solution.nabia04.room_database.DuesDetailDao
import mx.mobile.solution.nabia04.room_database.MainDataBase
import mx.mobile.solution.nabia04.room_database.entities.EntityYearlyDues
import mx.mobile.solution.nabia04.room_database.repositories.YearlyDuesRepository
import mx.mobile.solution.nabia04.utilities.BackgroundTasks
import java.text.SimpleDateFormat
import java.util.*


class FragmentYearlyDues() : BaseDataBindingFragment<FragmentYearlyPaymentBinding>() {

    private lateinit var list: List<EntityYearlyDues>
    private lateinit var dao: DuesDetailDao
    private lateinit var duesData: List<EntityYearlyDues>

    private val generator: ColorGenerator = ColorGenerator.DEFAULT

    private lateinit var adapter: ListAdapter1

    override fun getLayoutRes(): Int = R.layout.fragment_yearly_payment

    private lateinit var repository: YearlyDuesRepository

    companion object {
        private var selectedYear = ""
        fun newInstance(pos: Int): FragmentYearlyDues {
            selectedYear = excelHelper.getWorkbook().getSheetName(pos)
            return FragmentYearlyDues()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        repository = YearlyDuesRepository(requireContext())
        dao = MainDataBase.getDatabase(context).duesDetailsDao()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vb!!.recyclerView.setHasFixedSize(true)
        vb!!.recyclerView.layoutManager = LinearLayoutManager(requireActivity())
        adapter = ListAdapter1()
        vb!!.recyclerView.adapter = adapter
        getDataForThisYear()
    }

    private fun getDataForThisYear() {
        Log.i("TAG", "getDataForThisYear()..........")
        object : BackgroundTasks() {
            override fun onPreExecute() {
            }

            override fun doInBackground() {
                list = dao.getThisYearDues(selectedYear)
            }

            override fun onPostExecute() {
                Log.i("TAG", "List submitted to Adapter = ${list.size}")
                adapter.submitList(list)
                showProgress(false)
                adapter.notifyDataSetChanged()
            }

        }.execute()
    }

    private fun sortData() {
        Log.i("TAG", "sortData()..........")
        Log.i("TAG", "SORT = $SORT")
        object : BackgroundTasks() {
            val sortedList = list.toMutableList()
            override fun onPreExecute() {}
            override fun doInBackground() {

                if (sortedList.isEmpty()) {
                    return
                }

                when (SORT) {
                    1 -> {
                        sortedList.sortWith(fun(
                            obj1: EntityYearlyDues,
                            obj2: EntityYearlyDues
                        ): Int {
                            return obj1.name.compareTo(obj2.name)
                        })
                    }
                    2 -> {
                        val headerItem = sortedList[0]
                        val totalItem = sortedList[sortedList.size - 1]
                        sortedList.removeAt(0)
                        sortedList.removeAt(sortedList.size - 1)
                        sortedList.sortWith(fun(
                            obj1: EntityYearlyDues,
                            obj2: EntityYearlyDues
                        ): Int {
                            val amount1 = obj1.payments[12].toDouble()
                            val amount2 = obj2.payments[12].toDouble()
                            return amount2.compareTo(amount1)
                        })
                        sortedList.add(0, headerItem)
                        sortedList.add(sortedList.size, totalItem)
                    }
                    3 -> {
                        val headerItem = sortedList[0]
                        val totalItem = sortedList[sortedList.size - 1]
                        sortedList.removeAt(0)
                        sortedList.removeAt(sortedList.size - 1)
                        sortedList.sortWith(fun(
                            obj1: EntityYearlyDues,
                            obj2: EntityYearlyDues
                        ): Int {
                            val amount1 = obj1.payments[12].toDouble()
                            val amount2 = obj2.payments[12].toDouble()
                            return amount1.compareTo(amount2)
                        })
                        sortedList.add(0, headerItem)
                        sortedList.add(sortedList.size, totalItem)
                    }
                }
            }

            override fun onPostExecute() {
                Log.i("TAG", "List submitted to Adapter = ${sortedList.size}")
                adapter.submitList(sortedList)
                showProgress(false)
            }
        }.execute()
    }

    private fun showProgress(show: Boolean) {
        if (show) {
            vb!!.progressBar.visibility = View.VISIBLE
            vb!!.recyclerView.visibility = View.INVISIBLE
        } else {
            vb!!.progressBar.visibility = View.GONE
            vb!!.recyclerView.visibility = View.VISIBLE
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.dues_detail_menu, menu)

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.sort -> {
                val menuItemView: View =
                    ActivityCompat.requireViewById(requireActivity(), R.id.sort)
                showSortPopup(menuItemView)
                super.onOptionsItemSelected(item)
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showSortPopup(view: View) {

        val popupMenu = PopupMenu(requireContext(), view)

        popupMenu.menu.add(0, 1, 0, "Sort A - z")
        val amountSubMenu = popupMenu.menu.addSubMenu(1, 5, 1, "Sort by Amount")
        amountSubMenu.add(1, 2, 0, "Highest to Lowest")
        amountSubMenu.add(1, 3, 1, "Lowest to Highest")
        popupMenu.setOnMenuItemClickListener { item ->
            if (item.itemId < 5) {
                SORT = item.itemId
                sortData()
            }
            true
        }
        popupMenu.show()
    }


    private class ListAdapter1() :
        ListAdapter<EntityYearlyDues, ListAdapter1.MyViewHolder>(DiffCallback()) {
        /* ViewHolder for Flower, takes in the inflated view and the onClick behavior. */
        private val generator: ColorGenerator = ColorGenerator.DEFAULT

        inner class MyViewHolder(parent: View) : RecyclerView.ViewHolder(parent) {
            val index: TextView = itemView.findViewById(R.id.index)
            val name: TextView = itemView.findViewById(R.id.name)
            val folio: TextView = itemView.findViewById(R.id.folio)
            val itemHolder: View = itemView.findViewById(R.id.item_holder)
            val total: TextView = itemView.findViewById(R.id.total)
            private var payment: EntityYearlyDues? = null


            /* Bind flower name and image. */
            fun bind(flower: EntityYearlyDues, i: Int) {

                Log.i("TAG", "bind()..........$i")

                payment = flower

                val amounts = flower.payments

                index.text = i.toString()
                name.text = flower.name
                folio.text = flower.folio
                total.text = amounts[12]

                itemHolder.setBackgroundColor(generator.randomColor)

            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.yearly_dues_list_item, parent, false)
            return MyViewHolder(view)
        }

        /* Gets current flower and uses it to bind view. */
        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            val flower = getItem(position)
            holder.bind(flower, position)

        }

        private class DiffCallback : DiffUtil.ItemCallback<EntityYearlyDues>() {
            override fun areItemsTheSame(
                oldItem: EntityYearlyDues,
                newItem: EntityYearlyDues
            ): Boolean {
                val araTheSame = oldItem.folio == newItem.folio
                Log.i("TAG", "Items are the same: " + araTheSame)
                return araTheSame
            }

            override fun areContentsTheSame(
                oldItem: EntityYearlyDues,
                newItem: EntityYearlyDues
            ): Boolean {
                val araTheSame = oldItem.id == newItem.id
                Log.i("TAG", "Items are the same: " + araTheSame)
                return araTheSame
            }
        }

    }


    private inner class ListAdapter2() : RecyclerView.Adapter<ListAdapter2.MyViewHolder>() {
        private var payments: MutableList<EntityYearlyDues> = ArrayList()
        private val fd = SimpleDateFormat("EEE, d MMM yyyy hh:mm", Locale.US)

        fun upDateList(data: List<EntityYearlyDues>) {
            val diffCallback = ActorDiffCallback(this.payments, data)
            val diffResult = DiffUtil.calculateDiff(diffCallback)
            this.payments.clear()
            this.payments.addAll(data)
            diffResult.dispatchUpdatesTo(this)
        }

        inner class MyViewHolder(val parent: View) :
            RecyclerView.ViewHolder(parent) {
            val index: TextView = itemView.findViewById(R.id.index)
            val name: TextView = itemView.findViewById(R.id.name)
            val folio: TextView = itemView.findViewById(R.id.folio)
            val itemHolder: View = itemView.findViewById(R.id.item_holder)
            val total: TextView = itemView.findViewById(R.id.total)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.yearly_dues_list_item, parent, false)
            return MyViewHolder(view)
        }

        override fun onBindViewHolder(holder: MyViewHolder, i: Int) {

            val payment = payments[i]

            val amounts = payment.payments

            holder.index.text = i.toString()
            holder.name.text = payment.name
            holder.folio.text = payment.folio
            holder.total.text = amounts[12]

            holder.itemHolder.setBackgroundColor(generator.randomColor)

        }

        private fun getDate(l: Long): String {
            return fd.format(l)
        }

        override fun getItemCount(): Int {
            return payments.size
        }
    }

    class ActorDiffCallback(
        private val oldList: List<EntityYearlyDues>, private val newList: List<EntityYearlyDues>
    ) : DiffUtil.Callback() {

        override fun getOldListSize() = oldList.size

        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].name == newList[newItemPosition].name
        }

    }

}
