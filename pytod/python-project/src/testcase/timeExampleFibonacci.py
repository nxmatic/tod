import sys
from debugger.pytod.core.hunterTrace import hT
import time

class Fibonacci(object):
    
    def __init__(self):
        self.resultado = -1
        
    def fibonacci(self, n):
        if n == 1 or n == 2:
            self.resultado = 1
        elif n > 2:
            self.resultado = self.fibonacci(n-1) + self.fibonacci(n-2)
        return self.resultado

if __name__ == '__main__':
    first = time.time()    
    theNumero = 32
    theFibonacci = Fibonacci()
    print theFibonacci.fibonacci(theNumero)
    second = time.time()
    print second-first    