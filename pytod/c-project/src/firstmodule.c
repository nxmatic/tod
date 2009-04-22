#include <Python.h>

PyObject *exception = NULL;

PyObject *
AddCommand(PyObject *self, PyObject *args)
{   PyObject *result = NULL;
    long a, b;

    if(!PyArg_ParseTuple(args, "ii", &a, &b)){
        return result;
    }/*si es que existe un error en if
      *result se devolvera con valor NULL.
      */
    /*
    PyErr_SetString(exception, "No quiero trabajar, es feriado");
    return NULL;
     */
    result = Py_BuildValue("i",a + b);
    return result;
}

PyMethodDef methods[] = {
    {"add", AddCommand, METH_VARARGS},
    {NULL, NULL}
};

void 
initfirstmodule(void)
{   PyObject *dict, *module, *value;

    module = Py_InitModule("firstmodule",methods);
    if (module == NULL)
        return;
    dict = PyModule_GetDict(module);
    exception = PyErr_NewException("firstmodule.error", NULL, NULL);
    Py_INCREF(exception);
    PyModule_AddObject(module, "error", exception);
    PyDict_SetItemString(dict, "pyTOD", value);
    value = PyFloat_FromDouble(1.12346);
    Py_DECREF(value);
}

int 
main(int argc, char *argv[])
{
    Py_SetProgramName(argv[0]);
    Py_Initialize();
    initfirstmodule();
}
