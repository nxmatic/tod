#include <Python.h>

typedef struct{
    PyObject_HEAD
    long valor;
}counter;

#define Counter_Check(v) ((v)->ob_type == &counterType)
#define Counter_value(v) (((counter *)(v))->value)

/*creation and destruction*/

/*C constructor
 */
PyObject * counter_NEW(long valor_inicial)
{
    counter *object = NULL;
    object = PyObject_New(counter, &counterType);
    if (object != NULL)
        object->valor = valor_inicial;
    return (PyObject *)object;   
}

void
counter_dealloc(PyObject *self)
{
    PyMem_DEL(self);
}

PyObject *
counter_new(PyObject *self, PyObject *args)
{
    PyObject *object = NULL;
    long valor = 0;
    if (PyArg_ParseTuple(args,"|i",&valor))
    {
        object = counter_NEW(valor);
    }
    return object;
}

static PyMethodDef counter_methods[] = {
    {NULL} /*sentinela*/
};

static PyTypeObject counterType = {
    PyObject_HEAD_INIT(NULL)
    0,
    "counter",
    sizeof(counter),
    0,
    0,//counter_dealloc,
    0,//counter_print,
    0,//counter_getattr,
    0,//counter_setattr,
    0,//counter_compare,
    0,//counter_repr,
    0,//&counter_as_number,
    0,
    0,
    0,//counter_hash,
    0,
    0,//counter_str,
    0,
    0,
    0,
    Py_TPFLAGS_DEFAULT,
    "Objecto counter",
    0,
    0,
    0,
    0,
    0,
    0,
    0,
    0,
    0,
    0,
    0,
    0,
    0,
    0,
    (initproc)counter_init,
    counter_new,
    PyObject_GC_Del,
};



PyMODINIT_FUNC
initcounter(void) 
{
    PyObject *module;
    if (PyType_Ready(&counterType) < 0)
        return;
    module = Py_InitModule3("noddy",counter_methods,
    "Ejemplo de un modulo que crea una extension de tipo");
    Py_INCREF(&counterType);
    PyModule_AddObject(module, "counter",(PyObject *)&counterType);
}
