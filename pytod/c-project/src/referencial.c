#include <Python.h>

static PyObject *
referencial_parrot(PyObject *self, PyObject *args, PyObject *keywds)
{
    int voltage;
    char *state = "a";
    char *action = "a";
    char *type = "a";

    static char *kwlist[] = {"voltage","state", "action", "type", NULL};
    if (!PyArg_ParseTupleAndKeywords(args, keywds, "i|sss",kwlist,
                                                &voltage,&state,&action,&type))
        return NULL;

    printf("%s %i \n",action,voltage);
    printf("%s %s \n",state,type);
    Py_INCREF(Py_None);
    return Py_None;
}

static PyMethodDef referencial_methods[] = {
    {"parrot", (PyCFunction)referencial_parrot,
    METH_VARARGS | METH_KEYWORDS,
    "imprime algo"},
    {NULL,NULL,0,NULL}
};

PyMODINIT_FUNC initreferencial(void)
{
    (void) Py_InitModule("referencial", referencial_methods);
}

int
main (int argc, char *argv[])
{
    Py_SetProgramName(argv[0]);
    Py_Initialize();
    initreferencial();
}

/*
>>> import referencial
>>> help(referencial.parrot)

>>> referencial.parrot(1)
a 1 
a a 
>>> referencial.parrot(voltage=5)
a 5 
a a 
>>> referencial.parrot(5,'a','b','c')
b 5 
a c 
>>> referencial.parrot(voltage=5,state='a',action='b',type='c')
b 5 
a c 
>>> referencial.parrot(voltage=5,{state:'a',action:'b',type:'c'})
  File "<stdin>", line 1
SyntaxError: non-keyword arg after keyword arg
>>> referencial.parrot(voltage=5,{'state':'a','action':'b','type':'c'})
  File "<stdin>", line 1
SyntaxError: non-keyword arg after keyword arg
>>> referencial.parrot(voltage=5,state='a',action='b')
b 5 
a a 
 */
