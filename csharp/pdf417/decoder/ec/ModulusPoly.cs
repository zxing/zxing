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
using System.Text;

namespace ZXing.PDF417.Internal.EC
{
    /// <summary>
    /// <see cref="com.google.zxing.common.reedsolomon.GenericGFPoly"/>
    /// </summary>
    /// <author>Sean Owen</author>
    internal sealed class ModulusPoly
    {
        private readonly ModulusGF field;
        private readonly int[] coefficients;

        public ModulusPoly(ModulusGF field, int[] coefficients)
        {
            if (coefficients.Length == 0)
            {
                throw new ArgumentException();
            }
            this.field = field;
            int coefficientsLength = coefficients.Length;
            if (coefficientsLength > 1 && coefficients[0] == 0)
            {
                // Leading term must be non-zero for anything except the constant polynomial "0"
                int firstNonZero = 1;
                while (firstNonZero < coefficientsLength && coefficients[firstNonZero] == 0)
                {
                    firstNonZero++;
                }
                if (firstNonZero == coefficientsLength)
                {
                    this.coefficients = field.Zero.coefficients;
                } else
                {
                    this.coefficients = new int[coefficientsLength - firstNonZero];
                    Array.Copy(coefficients,
                   firstNonZero,
                   this.coefficients,
                   0,
                   this.coefficients.Length);
                }
            } else
            {
                this.coefficients = coefficients;
            }
        }

        /// <summary>
        /// Gets the coefficients.
        /// </summary>
        /// <value>The coefficients.</value>
        internal int[] Coefficients
        {
            get
            {
                return coefficients;
            }
        }

        /// <summary>
        /// degree of this polynomial
        /// </summary>
        internal int Degree
        {
            get
            {
                return coefficients.Length - 1;
            }
        }

        /// <summary>
        /// Gets a value indicating whether this instance is zero.
        /// </summary>
        /// <value>true if this polynomial is the monomial "0"
        /// </value>
        internal bool IsZero
        {
            get { return coefficients[0] == 0; }
        }

        /// <summary>
        /// coefficient of x^degree term in this polynomial
        /// </summary>
        /// <param name="degree">The degree.</param>
        /// <returns>coefficient of x^degree term in this polynomial</returns>
        internal int GetCoefficient(int degree)
        {
            return coefficients[coefficients.Length - 1 - degree];
        }

        /// <summary>
        /// evaluation of this polynomial at a given point
        /// </summary>
        /// <param name="a">A.</param>
        /// <returns>evaluation of this polynomial at a given point</returns>
        internal int EvaluateAt(int a)
        {
            if (a == 0)
            {
                // Just return the x^0 coefficient
                return GetCoefficient(0);
            }
            int size = coefficients.Length;
            int result = 0;
            if (a == 1)
            {
                // Just the sum of the coefficients
                foreach (var coefficient in coefficients)
                {
                    result = field.Add(result, coefficient);
                }
                return result;
            }
            result = coefficients[0];
            for (int i = 1; i < size; i++)
            {
                result = field.Add(field.Multiply(a, result), coefficients[i]);
            }
            return result;
        }

        /// <summary>
        /// Adds another Modulus
        /// </summary>
        /// <param name="other">Other.</param>
        internal ModulusPoly Add(ModulusPoly other)
        {
            if (!field.Equals(other.field))
            {
                throw new ArgumentException("ModulusPolys do not have same ModulusGF field");
            }
            if (IsZero)
            {
                return other;
            }
            if (other.IsZero)
            {
                return this;
            }

            int[] smallerCoefficients = this.coefficients;
            int[] largerCoefficients = other.coefficients;
            if (smallerCoefficients.Length > largerCoefficients.Length)
            {
                int[] temp = smallerCoefficients;
                smallerCoefficients = largerCoefficients;
                largerCoefficients = temp;
            }
            int[] sumDiff = new int[largerCoefficients.Length];
            int lengthDiff = largerCoefficients.Length - smallerCoefficients.Length;
            // Copy high-order terms only found in higher-degree polynomial's coefficients
            Array.Copy(largerCoefficients, 0, sumDiff, 0, lengthDiff);

            for (int i = lengthDiff; i < largerCoefficients.Length; i++)
            {
                sumDiff[i] = field.Add(smallerCoefficients[i - lengthDiff], largerCoefficients[i]);
            }

            return new ModulusPoly(field, sumDiff);
        }

        /// <summary>
        /// Subtract another Modulus
        /// </summary>
        /// <param name="other">Other.</param>
        internal ModulusPoly Subtract(ModulusPoly other)
        {
            if (!field.Equals(other.field))
            {
                throw new ArgumentException("ModulusPolys do not have same ModulusGF field");
            }
            if (other.IsZero)
            {
                return this;
            }
            return Add(other.GetNegative());
        }

        /// <summary>
        /// Multiply by another Modulus
        /// </summary>
        /// <param name="other">Other.</param>
        internal ModulusPoly Multiply(ModulusPoly other)
        {
            if (!field.Equals(other.field))
            {
                throw new ArgumentException("ModulusPolys do not have same ModulusGF field");
            }
            if (IsZero || other.IsZero)
            {
                return field.Zero;
            }
            int[] aCoefficients = this.coefficients;
            int aLength = aCoefficients.Length;
            int[] bCoefficients = other.coefficients;
            int bLength = bCoefficients.Length;
            int[] product = new int[aLength + bLength - 1];
            for (int i = 0; i < aLength; i++)
            {
                int aCoeff = aCoefficients[i];
                for (int j = 0; j < bLength; j++)
                {
                    product[i + j] = field.Add(product[i + j], field.Multiply(aCoeff, bCoefficients[j]));
                }
            }
            return new ModulusPoly(field, product);
        }

        /// <summary>
        /// Returns a Negative version of this instance
        /// </summary>
        internal ModulusPoly GetNegative()
        {
            int size = coefficients.Length;
            int[] negativeCoefficients = new int[size];
            for (int i = 0; i < size; i++)
            {
                negativeCoefficients[i] = field.Subtract(0, coefficients[i]);
            }
            return new ModulusPoly(field, negativeCoefficients);
        }

        /// <summary>
        /// Multiply by a Scalar.
        /// </summary>
        /// <param name="scalar">Scalar.</param>
        internal ModulusPoly Multiply(int scalar)
        {
            if (scalar == 0)
            {
                return field.Zero;
            }
            if (scalar == 1)
            {
                return this;
            }
            int size = coefficients.Length;
            int[] product = new int[size];
            for (int i = 0; i < size; i++)
            {
                product[i] = field.Multiply(coefficients[i], scalar);
            }
            return new ModulusPoly(field, product);
        }

        /// <summary>
        /// Multiplies by a Monomial
        /// </summary>
        /// <returns>The by monomial.</returns>
        /// <param name="degree">Degree.</param>
        /// <param name="coefficient">Coefficient.</param>
        internal ModulusPoly MultiplyByMonomial(int degree, int coefficient)
        {
            if (degree < 0)
            {
                throw new ArgumentException();
            }
            if (coefficient == 0)
            {
                return field.Zero;
            }
            int size = coefficients.Length;
            int[] product = new int[size + degree];
            for (int i = 0; i < size; i++)
            {
                product[i] = field.Multiply(coefficients[i], coefficient);
            }
            return new ModulusPoly(field, product);
        }

        /// <summary>
        /// Divide by another modulus
        /// </summary>
        /// <param name="other">Other.</param>
        internal ModulusPoly[] Divide(ModulusPoly other)
        {
            if (!field.Equals(other.field))
            {
                throw new ArgumentException("ModulusPolys do not have same ModulusGF field");
            }
            if (other.IsZero)
            {
                throw new DivideByZeroException();
            }

            ModulusPoly quotient = field.Zero;
            ModulusPoly remainder = this;

            int denominatorLeadingTerm = other.GetCoefficient(other.Degree);
            int inverseDenominatorLeadingTerm = field.Inverse(denominatorLeadingTerm);

            while (remainder.Degree >= other.Degree && !remainder.IsZero)
            {
                int degreeDifference = remainder.Degree - other.Degree;
                int scale = field.Multiply(remainder.GetCoefficient(remainder.Degree), inverseDenominatorLeadingTerm);
                ModulusPoly term = other.MultiplyByMonomial(degreeDifference, scale);
                ModulusPoly iterationQuotient = field.BuildMonomial(degreeDifference, scale);
                quotient = quotient.Add(iterationQuotient);
                remainder = remainder.Subtract(term);
            }

            return new ModulusPoly[] { quotient, remainder };
        }

        /// <summary>
        /// Returns a <see cref="System.String"/> that represents the current <see cref="ZXing.PDF417.Internal.EC.ModulusPoly"/>.
        /// </summary>
        /// <returns>A <see cref="System.String"/> that represents the current <see cref="ZXing.PDF417.Internal.EC.ModulusPoly"/>.</returns>
        override public String ToString()
        {
            var result = new StringBuilder(8 * Degree);
            for (int degree = Degree; degree >= 0; degree--)
            {
                int coefficient = GetCoefficient(degree);
                if (coefficient != 0)
                {
                    if (coefficient < 0)
                    {
                        result.Append(" - ");
                        coefficient = -coefficient;
                    } else
                    {
                        if (result.Length > 0)
                        {
                            result.Append(" + ");
                        }
                    }
                    if (degree == 0 || coefficient != 1)
                    {
                        result.Append(coefficient);
                    }
                    if (degree != 0)
                    {
                        if (degree == 1)
                        {
                            result.Append('x');
                        } else
                        {
                            result.Append("x^");
                            result.Append(degree);
                        }
                    }
                }
            }
            return result.ToString();
        }
    }
}
