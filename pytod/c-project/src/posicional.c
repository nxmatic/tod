#include <Python.h>

static PyObject *
posicional_parrot(PyObject *self, PyObject *args)
{
    int voltage;
    char *state = "state";
    char *action = "action";
    char *type = "type";

    if(!PyArg_ParseTuple(args,"i|sss",&voltage,&state,&action,&type))
        return NULL;

    printf("%s %i \n",action,voltage);
    printf("%s %s \n",state,type);
    Py_INCREF(Py_None);
    return Py_None;
}

static PyMethodDef posicional_methods[] = {
    {"parrot", (PyCFunction)posicional_parrot,
    METH_VARARGS | METH_KEYWORDS,
    "imprime algo"},
    {NULL,NULL,0,NULL}
};

PyMODINIT_FUNC initposicional(void)
{
    (void) Py_InitModule("posicional", posicional_methods);
}

int
main (int argc, char *argv[])
{
    Py_SetProgramName(argv[0]);
    Py_Initialize();
    initposicional();
}

/*
>>> posicional.parrot(1)
action 1 
state type 
>>> posicional.parrot(1,'a','b','c')
b 1 
a c 
>>> posicional.parrot(voltage=4)
Traceback (most recent call last):
  File "<stdin>", line 1, in <module>
TypeError: function takes at least 1 argument (0 given)
 */
