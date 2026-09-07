package com.example.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ProductivityApplication
import com.example.R
import com.example.data.TaskEntity
import com.example.data.TaskListEntity
import com.example.databinding.DialogAddTaskBinding
import com.example.databinding.FragmentTasksBinding
import com.example.ui.adapters.ActiveTasksAdapter
import com.example.ui.adapters.CompletedTasksAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch

class TasksFragment : Fragment() {

    private var _binding: FragmentTasksBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProductivityViewModel by lazy {
        val app = requireActivity().application as ProductivityApplication
        ViewModelProvider(requireActivity(), ProductivityViewModelFactory(app.repository))[ProductivityViewModel::class.java]
    }

    private lateinit var activeTasksAdapter: ActiveTasksAdapter
    private lateinit var completedTasksAdapter: CompletedTasksAdapter

    private var currentLists: List<TaskListEntity> = emptyList()
    private var isUpdatingTabs = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTasksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        setupTabLayout()
        setupHeaderActions()
        setupCompletedSectionToggle()
        setupFab()
        observeData()
    }

    private fun setupRecyclerViews() {
        activeTasksAdapter = ActiveTasksAdapter(
            onToggleComplete = { task ->
                viewModel.toggleTaskCompletion(task)
            },
            onToggleStar = { task ->
                viewModel.toggleTaskStarred(task)
            },
            onItemClick = { task ->
                showEditTaskDialog(task)
            },
            onItemLongClick = { task ->
                showTaskOptionsDialog(task)
            }
        )

        completedTasksAdapter = CompletedTasksAdapter(
            onUncompleteClick = { task ->
                viewModel.toggleTaskCompletion(task)
            },
            onDeleteClick = { task ->
                showDeleteTaskConfirmDialog(task)
            }
        )

        binding.recyclerActiveTasks.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = activeTasksAdapter
        }

        binding.recyclerCompletedTasks.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = completedTasksAdapter
        }
    }

    private fun setupTabLayout() {
        binding.tabLayoutTaskLists.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (isUpdatingTabs || tab == null) return
                val pos = tab.position

                if (pos == 0) {
                    // Starred tasks tab
                    viewModel.selectStarredTab()
                } else if (pos == currentLists.size + 1) {
                    // + New list button
                    showCreateListDialog()
                    // Keep previous selection active until new list is created
                    restoreSelectedTabPosition()
                } else {
                    // List index pos - 1
                    val listIndex = pos - 1
                    if (listIndex in currentLists.indices) {
                        viewModel.selectList(currentLists[listIndex].id)
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {
                if (tab?.position == currentLists.size + 1) {
                    showCreateListDialog()
                }
            }
        })
    }

    private fun setupHeaderActions() {
        binding.btnSortTasks.setOnClickListener {
            showSortMenu()
        }

        binding.btnListMenu.setOnClickListener { view ->
            showListOptionsMenu(view)
        }
    }

    private fun setupCompletedSectionToggle() {
        binding.btnToggleCompleted.setOnClickListener {
            viewModel.toggleCompletedSection()
        }
    }

    private fun setupFab() {
        binding.fabAddTask.setOnClickListener {
            showAddTaskDialog()
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 1. Observe Lists and sync tabs
                launch {
                    viewModel.allTaskLists.collect { lists ->
                        currentLists = lists
                        renderTabs(lists)
                    }
                }

                // 2. Observe Tasks
                launch {
                    viewModel.currentTasks.collect { tasks ->
                        val activeTasks = tasks.filter { !it.isCompleted }
                        val completedTasks = tasks.filter { it.isCompleted }

                        activeTasksAdapter.submitList(activeTasks)
                        completedTasksAdapter.submitList(completedTasks)

                        // Active tasks empty state
                        if (activeTasks.isEmpty()) {
                            binding.layoutEmptyTasks.visibility = View.VISIBLE
                            if (viewModel.isStarredTabSelected.value) {
                                binding.textEmptyTitle.text = getString(R.string.no_starred_tasks)
                                binding.textEmptyHint.text = getString(R.string.no_starred_tasks_hint)
                            } else {
                                binding.textEmptyTitle.text = getString(R.string.no_tasks_in_list)
                                binding.textEmptyHint.text = getString(R.string.no_tasks_hint)
                            }
                        } else {
                            binding.layoutEmptyTasks.visibility = View.GONE
                        }

                        // Completed tasks header and visibility
                        binding.textCompletedHeader.text = getString(R.string.completed_header_format, completedTasks.size)
                        binding.cardCompletedTasks.visibility = if (completedTasks.isNotEmpty()) View.VISIBLE else View.GONE
                    }
                }

                // 3. Observe selected list or starred tab to update header
                launch {
                    viewModel.isStarredTabSelected.collect { isStarred ->
                        updateHeaderTitle()
                        restoreSelectedTabPosition()
                    }
                }

                launch {
                    viewModel.selectedListId.collect {
                        updateHeaderTitle()
                        restoreSelectedTabPosition()
                    }
                }

                // 4. Observe completed section expanded state
                launch {
                    viewModel.isCompletedSectionExpanded.collect { isExpanded ->
                        binding.recyclerCompletedTasks.visibility = if (isExpanded) View.VISIBLE else View.GONE
                        binding.iconExpandCompleted.setImageResource(
                            if (isExpanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more
                        )
                    }
                }
            }
        }
    }

    private fun renderTabs(lists: List<TaskListEntity>) {
        isUpdatingTabs = true
        val tabLayout = binding.tabLayoutTaskLists
        tabLayout.removeAllTabs()

        // Tab 0: Starred icon
        val starTab = tabLayout.newTab().apply {
            text = "★"
            contentDescription = "Starred Tasks"
        }
        tabLayout.addTab(starTab)

        // Tabs 1..N: Task lists
        for (list in lists) {
            val listTab = tabLayout.newTab().apply {
                text = list.name
            }
            tabLayout.addTab(listTab)
        }

        // Tab N+1: + New list
        val newTab = tabLayout.newTab().apply {
            text = getString(R.string.new_list)
        }
        tabLayout.addTab(newTab)

        isUpdatingTabs = false
        restoreSelectedTabPosition()
    }

    private fun restoreSelectedTabPosition() {
        if (isUpdatingTabs) return
        isUpdatingTabs = true

        val tabLayout = binding.tabLayoutTaskLists
        if (viewModel.isStarredTabSelected.value) {
            tabLayout.getTabAt(0)?.select()
        } else {
            val targetId = viewModel.selectedListId.value
            var selectedIndex = 1 // default to first list tab
            if (targetId != null) {
                val foundIndex = currentLists.indexOfFirst { it.id == targetId }
                if (foundIndex >= 0) {
                    selectedIndex = foundIndex + 1
                }
            }
            if (selectedIndex < tabLayout.tabCount - 1) {
                tabLayout.getTabAt(selectedIndex)?.select()
            }
        }

        isUpdatingTabs = false
    }

    private fun updateHeaderTitle() {
        if (viewModel.isStarredTabSelected.value) {
            binding.textListTitle.text = "Starred"
            binding.btnListMenu.visibility = View.GONE
        } else {
            binding.btnListMenu.visibility = View.VISIBLE
            val targetId = viewModel.selectedListId.value
            val currentList = currentLists.find { it.id == targetId }
                ?: currentLists.firstOrNull { it.isDefault }
                ?: currentLists.firstOrNull()
            binding.textListTitle.text = currentList?.name ?: getString(R.string.my_tasks)
        }
    }

    private fun showSortMenu() {
        val options = arrayOf(
            getString(R.string.sort_my_order),
            getString(R.string.sort_starred),
            getString(R.string.sort_alphabetical),
            getString(R.string.sort_newest)
        )

        val currentOrder = viewModel.taskSortOrder.value
        val checkedItem = when (currentOrder) {
            TaskSortOrder.MY_ORDER -> 0
            TaskSortOrder.STARRED_FIRST -> 1
            TaskSortOrder.ALPHABETICAL -> 2
            TaskSortOrder.NEWEST_FIRST -> 3
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.sort_by)
            .setSingleChoiceItems(options, checkedItem) { dialog, which ->
                val newOrder = when (which) {
                    0 -> TaskSortOrder.MY_ORDER
                    1 -> TaskSortOrder.STARRED_FIRST
                    2 -> TaskSortOrder.ALPHABETICAL
                    3 -> TaskSortOrder.NEWEST_FIRST
                    else -> TaskSortOrder.MY_ORDER
                }
                viewModel.setSortOrder(newOrder)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showListOptionsMenu(anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)
        val targetId = viewModel.selectedListId.value
        val currentList = currentLists.find { it.id == targetId }
            ?: currentLists.firstOrNull { it.isDefault }
            ?: currentLists.firstOrNull()

        popup.menu.add(0, 1, 0, getString(R.string.rename_list))
        popup.menu.add(0, 2, 1, getString(R.string.delete_completed_tasks))

        // If it's a custom user list (not default), allow deleting the list
        if (currentList != null && !currentList.isDefault) {
            popup.menu.add(0, 3, 2, getString(R.string.delete_list))
        }

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> {
                    showRenameListDialog(currentList?.name ?: "")
                    true
                }
                2 -> {
                    viewModel.deleteCompletedTasksInCurrentList()
                    Toast.makeText(requireContext(), "Deleted completed tasks", Toast.LENGTH_SHORT).show()
                    true
                }
                3 -> {
                    showDeleteListConfirmDialog(currentList?.name ?: "")
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun showCreateListDialog() {
        val input = EditText(requireContext()).apply {
            hint = getString(R.string.list_name_hint)
            setPadding(48, 32, 48, 32)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.app_text_primary))
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.create_new_list)
            .setView(input)
            .setPositiveButton(R.string.save) { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    viewModel.createNewList(name)
                } else {
                    Toast.makeText(requireContext(), "List name cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showRenameListDialog(currentName: String) {
        val input = EditText(requireContext()).apply {
            hint = getString(R.string.list_name_hint)
            setText(currentName)
            setSelection(currentName.length)
            setPadding(48, 32, 48, 32)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.app_text_primary))
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.rename_list)
            .setView(input)
            .setPositiveButton(R.string.save) { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    viewModel.renameCurrentList(newName)
                } else {
                    Toast.makeText(requireContext(), "List name cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showDeleteListConfirmDialog(listName: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_list)
            .setMessage("Are you sure you want to delete \"$listName\" and all its tasks?")
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteCurrentList()
                Toast.makeText(requireContext(), "List deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showAddTaskDialog() {
        val dialogBinding = DialogAddTaskBinding.inflate(layoutInflater)
        dialogBinding.textDialogTitle.text = getString(R.string.add_task_dialog_title)
        dialogBinding.checkboxStarTask.isChecked = viewModel.isStarredTabSelected.value

        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.save) { _, _ ->
                val title = dialogBinding.editTaskTitle.text.toString().trim()
                val notes = dialogBinding.editTaskNotes.text.toString().trim()
                val isStarred = dialogBinding.checkboxStarTask.isChecked

                if (title.isNotEmpty()) {
                    viewModel.addTask(title, notes, isStarred)
                } else {
                    Toast.makeText(requireContext(), "Task title cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showEditTaskDialog(task: TaskEntity) {
        val dialogBinding = DialogAddTaskBinding.inflate(layoutInflater)
        dialogBinding.textDialogTitle.text = "Edit task"
        dialogBinding.editTaskTitle.setText(task.title)
        dialogBinding.editTaskNotes.setText(task.notes)
        dialogBinding.checkboxStarTask.isChecked = task.isStarred

        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.save) { _, _ ->
                val title = dialogBinding.editTaskTitle.text.toString().trim()
                val notes = dialogBinding.editTaskNotes.text.toString().trim()
                val isStarred = dialogBinding.checkboxStarTask.isChecked

                if (title.isNotEmpty()) {
                    viewModel.updateTask(
                        task.copy(
                            title = title,
                            notes = notes,
                            isStarred = isStarred
                        )
                    )
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showTaskOptionsDialog(task: TaskEntity) {
        val options = arrayOf("Edit task", "Delete task")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(task.title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditTaskDialog(task)
                    1 -> showDeleteTaskConfirmDialog(task)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showDeleteTaskConfirmDialog(task: TaskEntity) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete task")
            .setMessage("Are you sure you want to delete \"${task.title}\"?")
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteTask(task.id)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "TasksFragment"
    }
}
