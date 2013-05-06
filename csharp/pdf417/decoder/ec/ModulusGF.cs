/*
 * Copyright 2012 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

using System;

namespace ZXing.PDF417.Internal.EC
{
    /// <summary>
    /// <p>A field based on powers of a generator integer, modulo some modulus.</p>
    /// @see com.google.zxing.common.reedsolomon.GenericGF
    /// </summary>
    /// <author>Sean Owen</author>
    /// <author>Stephen Furlani (CS Port of Guenther Grau's new PDF417 code)</author>
    internal sealed class ModulusGF
    {
        public static ModulusGF PDF417_GF = new ModulusGF(929, 3);

        private readonly int[] expTable;
        private readonly int[] logTable;
        public ModulusPoly Zero { get; private set; }
        public ModulusPoly One { get; private set; }
        private int Modulus { get; set; }

        public ModulusGF(int modulus, int generator)
        {
            this.Modulus = modulus;
            expTable = new int[modulus];
            logTable = new int[modulus];
            int x = 1;
            for (int i = 0; i < modulus; i++)
            {
                expTable[i] = x;
                x = (x * generator) % modulus;
            }
            for (int i = 0; i < modulus - 1; i++)
            {
                logTable[expTable[i]] = i;
            }
            // logTable[0] == 0 but this should never be used
            Zero = new ModulusPoly(this, new int[] { 0 });
            One = new ModulusPoly(this, new int[] { 1 });
        }

        internal ModulusPoly BuildMonomial(int degree, int coefficient)
        {
            if (degree < 0)
            {
                throw new ArgumentException();
            }
            if (coefficient == 0)
            {
                return Zero;
            }
            int[] coefficients = new int[degree + 1];
            coefficients[0] = coefficient;
            return new ModulusPoly(this, coefficients);
        }

        internal int Add(int a, int b)
        {
            return (a + b) % Modulus;
        }

        internal int Subtract(int a, int b)
        {
            return (Modulus + a - b) % Modulus;
        }

        internal int Exp(int a)
        {
            return expTable[a];
        }

        internal int Log(int a)
        {
            if (a == 0)
            {
                throw new ArgumentException();
            }
            return logTable[a];
        }

        internal int Inverse(int a)
        {
            if (a == 0)
            {
                throw new ArithmeticException();
            }
            return expTable[Modulus - logTable[a] - 1];
        }

        internal int Multiply(int a, int b)
        {
            if (a == 0 || b == 0)
            {
                return 0;
            }
            return expTable[(logTable[a] + logTable[b]) % (Modulus - 1)];
        }

        internal int Size
        {
            get
            {
                return Modulus;
            }
        }
    }
}
