#import <Python.h>

typedef struct{
    PyObject_HEAD
    /* Type-specific fields go here.*/
}noddy_NoddyObject;

static PyTypeObject noddy_NoddyType = {
    PyObject_HEAD_INIT(NULL)
    0,
    "noddy.Noddy",
    sizeof(noddy_NoddyObject),
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
    0,
    0,
    Py_TPFLAGS_DEFAULT,
    "Objecto Noddy",
};

static PyMethodDef noddy_methods[] = {
    {NULL} /*sentinela*/
};

#ifndef PyMODINIT_FUNC
#define PyMODINIT_FUNC void
#endif
PyMODINIT_FUNC
initnoddy(void)
{
    PyObject *module;
    noddy_NoddyType.tp_new = PyType_GenericNew;
    if (PyType_Ready(&noddy_NoddyType) < 0)
        return;
    module = Py_InitModule3("noddy",noddy_methods,
    "Ejemplo de un modulo que crea una extension de tipo");
    Py_INCREF(&noddy_NoddyType);
    PyModule_AddObject(module, "Noddy",(PyObject *)&noddy_NoddyType);
}


