package com.example.dataobjects

abstract class BaseEntity {
    abstract val date_created : Long
    abstract var date_updated : Long
    abstract val is_deleted : Boolean
    abstract var uid : String
}