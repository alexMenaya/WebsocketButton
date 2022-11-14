package com.example.dataobjects

abstract class Place {
    abstract val country_code : String
    abstract val date_created : Long
    abstract var date_updated : Long
    abstract val display_name : String
    abstract val id_region : Long
    abstract var is_deleted : Boolean
    abstract var latitude : Float
    abstract var longitude : Float
    abstract var num_hollers : Long
    abstract var radius : Float
    abstract val uid : Long
    //abstract var hollers : Set<Holler>
}