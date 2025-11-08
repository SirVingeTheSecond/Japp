package com.japp.models

import kotlinx.serialization.Serializable

@Serializable
data class GroupMembers(val members: List<User>, val groups: List<Group>)