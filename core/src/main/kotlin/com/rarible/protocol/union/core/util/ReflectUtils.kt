package com.rarible.protocol.union.core.util

import java.lang.reflect.ParameterizedType

class ReflectUtils {

    companion object {
        fun getGenericInterfaceType(instance: Any, genericInterface: Class<*>): Class<*> {
            var currentClass = instance.javaClass
            do {
                val generic = currentClass.genericInterfaces.find { infc ->
                    if (infc is ParameterizedType) {
                        infc.rawType == genericInterface
                    } else {
                        false
                    }
                }
                if (generic != null) {
                    val ruleClass = (generic as ParameterizedType).actualTypeArguments[0]
                    return ruleClass as Class<*>
                }
                val parent = currentClass.superclass
                currentClass = parent
            } while (currentClass != null)
            throw IllegalArgumentException("${instance.javaClass} doesn't implement interface $genericInterface")
        }
    }
}