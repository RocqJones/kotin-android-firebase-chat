package com.intoverflown.samplechatapp

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.intoverflown.samplechatapp.databinding.ActivityMainBinding
import com.intoverflown.samplechatapp.fragments.ChatFragment
import com.intoverflown.samplechatapp.fragments.ProfileFragment
import com.intoverflown.samplechatapp.fragments.UsersFragment
import com.intoverflown.samplechatapp.model.Chat
import com.intoverflown.samplechatapp.model.User
import java.util.*

class MainActivity : AppCompatActivity() {

    var firebaseUser: FirebaseUser? = null
    lateinit var reference: DatabaseReference

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view  = binding.root
        setContentView(view)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = ""

        firebaseUser = FirebaseAuth.getInstance().currentUser

        // Mendapatkan List Pengguna
        reference = FirebaseDatabase.getInstance().getReference("Users").child(firebaseUser?.uid!!)
        reference.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(e: DatabaseError) {
                Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_LONG).show()
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val user = dataSnapshot.getValue(User::class.java)
                binding.username.text = user?.username

                if (user?.imageURL.equals("default")){
                    binding.profileImage.setImageResource(R.mipmap.ic_launcher)
                } else {
                    //Change this
                    Glide.with(applicationContext).load(user?.imageURL).into(binding.profileImage)
                }

            }

        })

        // Mendapatkan List Pesan
        reference = FirebaseDatabase.getInstance().getReference("Chats")
        reference.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(dataError: DatabaseError) {
                // do something if databse error
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val viewPageAdapter = ViewPagerAdapter(supportFragmentManager)
                // get number of unreaded message
                var unread = 0
                for (snapshot in dataSnapshot.children) {
                    val chat = snapshot.getValue(Chat::class.java)
                    if (chat?.receiver?.equals(firebaseUser!!.uid)!! && !chat?.isseen!!){
                        unread++
                    }
                }
                if (unread == 0){
                    viewPageAdapter.addFragment(ChatFragment(), "Chats")
                } else {
                    viewPageAdapter.addFragment(ChatFragment(), "($unread) Chats")
                }

                // Tambah tab
                viewPageAdapter.addFragment(UsersFragment(), "Users")
                viewPageAdapter.addFragment(ProfileFragment(), "Profile")

                // Set adapter
                binding.viewPager.adapter = viewPageAdapter
                binding.tabLayout.setupWithViewPager(binding.viewPager)
            }

        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId){
            R.id.logout -> {
                FirebaseAuth.getInstance().signOut()
                //change this code cause your app will crash
                startActivity(Intent(this, LoginActivity::class.java))
                return true
            }
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return false
    }

    fun status(status: String){
        // get user
        reference = FirebaseDatabase.getInstance().getReference("Users").child(firebaseUser?.uid.toString())

        // prepare for data
        val hasMap: java.util.HashMap<String, Any> = hashMapOf()
        hasMap.put("status", status)

        // update databse
        reference.updateChildren(hasMap)
    }

    inner class ViewPagerAdapter(fm: FragmentManager): FragmentPagerAdapter(fm) {

        var fragments: ArrayList<Fragment>? = null
        var titles: ArrayList<String>? = null

        init {
            this.fragments = ArrayList()
            this.titles = ArrayList()
        }

        fun addFragment(fragment: Fragment, title: String) {
            fragments?.add(fragment)
            titles?.add(title)
        }
        override fun getItem(position: Int): Fragment {
            return fragments!![position]
        }

        override fun getCount(): Int {
            return fragments?.size!!
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return titles!![position]
        }

    }

    override fun onResume() {
        super.onResume()
        status("online")
    }

    override fun onPause() {
        super.onPause()
        status("offline")
    }
}