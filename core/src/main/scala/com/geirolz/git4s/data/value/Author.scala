package com.geirolz.git4s.data.value

opaque type Author = String
object Author:
  def apply(value: String): Author         = value
  extension (id: Author) def value: String = id
