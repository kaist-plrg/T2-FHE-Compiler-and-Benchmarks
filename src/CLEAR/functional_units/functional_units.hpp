#ifndef FUNCTIONAL_UNITS_HPP_
#define FUNCTIONAL_UNITS_HPP_

#include <vector>
#include <functional>
#include <algorithm>
#include <type_traits>

using namespace std;

// Generic function to apply binary operation to two vectors
// TODO: The returned vector should be of the same size as the smaller vector.
template<typename T1, typename T2, typename F>
auto apply_binop(const std::vector<T1>& a, const std::vector<T2>& b, F op) {
    using ResultType = typename std::common_type<T1, T2>::type;

    size_t min_size = std::min(a.size(), b.size());
    std::vector<ResultType> result(min_size);

    for (size_t i = 0; i < min_size; ++i) {
        result[i] = op(a[i], b[i]);
    }
    return result;
}

template<typename T1, typename T2, typename F>
void apply_binop_inplace(vector<T1>& a, const vector<T2>& b, F op) {
    size_t min_size = min(a.size(), b.size());

    for (size_t i = 0; i < min_size; ++i) {
        a[i] = op(a[i], b[i]);
    }
}

// + overloading
template<typename T1, typename T2>
auto operator+(const vector<T1>& a, const vector<T2>& b) {
    return apply_binop(a, b, plus<>());
}

template<typename T1, typename T2>
auto operator+(const vector<T1>& a, const T2& b) {
    vector<T2> b_vector(a.size(), b);
    return apply_binop(a, b_vector, plus<>());
}

template<typename T1, typename T2>
auto operator+(const T1& a, const vector<T2>& b) {
    vector<T1> a_vector(b.size(), a);
    return apply_binop(a_vector, b, plus<>());
}

// - overloading
template<typename T1, typename T2>
auto operator-(const vector<T1>& a, const vector<T2>& b) {
    return apply_binop(a, b, minus<>());
}

template<typename T1, typename T2>
auto operator-(const vector<T1>& a, const T2& b) {
    vector<T2> b_vector(a.size(), b);
    return apply_binop(a, b_vector, minus<>());
}

template<typename T1, typename T2>
auto operator-(const T1& a, const vector<T2>& b) {
    vector<T1> a_vector(b.size(), a);
    return apply_binop(a_vector, b, minus<>());
}

// * overloading
template<typename T1, typename T2>
auto operator*(const vector<T1>& a, const vector<T2>& b) {
    return apply_binop(a, b, multiplies<>());
}

template<typename T1, typename T2>
auto operator*(const vector<T1>& a, const T2& b) {
    vector<T2> b_vector(a.size(), b);
    return apply_binop(a, b_vector, multiplies<>());
}

template<typename T1, typename T2>
auto operator*(const T1& a, const vector<T2>& b) {
    vector<T1> a_vector(b.size(), a);
    return apply_binop(a_vector, b, multiplies<>());
}


// += overloading
template<typename T1, typename T2>
auto operator+=(vector<T1>& a, const vector<T2>& b) {
    apply_binop_inplace(a, b, plus<>());
    return a;
}

template<typename T1, typename T2>
auto operator+=(vector<T1>& a, const T2& b) {
    vector<T2> b_vector(a.size(), b);
    apply_binop_inplace(a, b_vector, plus<>());
    return a;
}

// -= overloading
template<typename T1, typename T2>
auto operator-=(vector<T1>& a, const vector<T2>& b) {
    apply_binop_inplace(a, b, minus<>());
    return a;
}

template<typename T1, typename T2>
auto operator-=(vector<T1>& a, const T2& b) {
    vector<T2> b_vector(a.size(), b);
    apply_binop_inplace(a, b_vector, minus<>());
    return a;
}

// *= overloading
template<typename T1, typename T2>
auto operator*=(vector<T1>& a, const vector<T2>& b) {
    apply_binop_inplace(a, b, multiplies<>());
    return a;
}

template<typename T1, typename T2>
auto operator*=(vector<T1>& a, const T2& b) {
    vector<T2> b_vector(a.size(), b);
    apply_binop_inplace(a, b_vector, multiplies<>());
    return a;
}

// ++ overloading
template<typename T>
vector<T> operator++(vector<T>& a, int) {
    for (auto& item : a) {
        ++item;
    }
    return a;
}

// -- overloading
template<typename T>
vector<T>& operator--(vector<T>& a, int) {
    for (auto& item : a) {
        --item;
    }
    return a;
}

// rotateLeft and rotateRight
template <typename T>
void rotateLeft(std::vector<T>& v, int n) {
    if (!v.empty()) {
        std::rotate(v.begin(), v.begin() + n % v.size(), v.end());
    }
}

template <typename T>
void rotateRight(std::vector<T>& v, int n) {
    if (!v.empty()) {
        std::rotate(v.begin(), v.end() - n % v.size(), v.end());
    }
}

#endif  // FUNCTIONAL_UNITS_HPP_
