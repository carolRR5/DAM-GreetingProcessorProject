package com.example.app

fun main() {
    val myClass = MyClass()
    val wrappedMyClass = MyClassWrapper(myClass) // Use the wrapper class

    wrappedMyClass.sayHello()
    wrappedMyClass.compute()
}