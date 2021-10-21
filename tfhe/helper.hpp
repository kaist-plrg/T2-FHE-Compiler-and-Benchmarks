#ifndef HELPER_HPP_
#define HELPER_HPP_

#include <tfhe/tfhe.h>
#include <tfhe/tfhe_io.h>

/// Check if n is a power of 2.
bool is_pow_of_2(int n);

/// Encypt a number with the cloud key. Return result = Enc(ptxt_val).
LweSample* enc_cloud(uint32_t ptxt_val, size_t word_sz,
                     const TFheGateBootstrappingCloudKeySet* bk);

/// Adder circuit: result = a + b.
void add(LweSample* result, const LweSample* a, const LweSample* b,
         const size_t nb_bits, const TFheGateBootstrappingCloudKeySet* bk);

/// Adder circuit: a += b.
void add_inplace(LweSample* a, const LweSample* b, const size_t nb_bits,
                 const TFheGateBootstrappingCloudKeySet* bk);

/// Multiplier circuit: result = a * b.
void mult(LweSample* result, const LweSample* a, const LweSample* b, 
          const size_t nb_bits, const TFheGateBootstrappingCloudKeySet* bk);

/// Multiplier circuit: a *= b.
void mult_inplace(LweSample* a, const LweSample* b, const size_t nb_bits,
                  const TFheGateBootstrappingCloudKeySet* bk);

/// Incrementer circuit: result = a + 1.
void inc(LweSample* result, const LweSample* a, const size_t nb_bits, 
         const TFheGateBootstrappingCloudKeySet* bk);

/// Increment ciphertext a by 1 and store result to a. a++.
void inc_inplace(LweSample* a, const size_t nb_bits, 
                 const TFheGateBootstrappingCloudKeySet* bk);

/// General comparator: result = (a == b).
void cmp(LweSample* result, const LweSample* a, const LweSample* b,
         const size_t word_sz, const TFheGateBootstrappingCloudKeySet* bk);

/// Equality circuit: result = (a == b).
void eq(LweSample* result, const LweSample* a, const LweSample* b,
        const size_t word_sz, const TFheGateBootstrappingCloudKeySet* bk);

/// Comparator circuit: result = (a <= b).
void leq(LweSample* result, const LweSample* a, const LweSample* b,
         const size_t word_sz, const TFheGateBootstrappingCloudKeySet* bk);

#endif  // HELPER_HPP_
