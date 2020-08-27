////////////////////////////////////////////////////////////////////////////////
///
/// \file Error.hpp
/// ---------------
///
/// Copyright (c) 2017. Microblink Ltd. All rights reserved.
///
////////////////////////////////////////////////////////////////////////////////
//------------------------------------------------------------------------------
#pragma once

#include <Log.h>
#include <Utils/disable_warnings.hpp>

#include <boost/assert.hpp>
#include <boost/core/ignore_unused.hpp>
MB_DISABLE_WARNING_PUSH
MB_DISABLE_WARNING_GCC( "-Wattributes" )
#include <boost/err/result_or_error.hpp>
MB_DISABLE_WARNING_POP

#ifndef NDEBUG
#include <exception>
#endif // NDEBUG
#include <string>
//------------------------------------------------------------------------------
namespace pping
{
//------------------------------------------------------------------------------

#if defined( NDEBUG ) || defined( PLATFORM_IOS ) || ( defined( __ANDROID__ ) && defined( __arm__ ) ) || defined( BOOST_NO_EXCEPTIONS )
    enum struct Failure : std::uint8_t { Failed };
#else
    using Failure = std::exception_ptr;
#endif // NDEBUG

template <typename Result>
using Fallible = boost::err::result_or_error
<
    Result,
    Failure
>;

template <typename T> class Ref;

template <typename Result>
using FallibleRef = Fallible<Ref<Result>>;

/// Returns a generic failure or the current exception (depending on the build
/// configuration).
inline Failure failure() noexcept
{
#if defined( NDEBUG ) || defined( PLATFORM_IOS ) || ( defined( __ANDROID__ ) && defined( __arm__ ) ) || defined( BOOST_NO_EXCEPTIONS )
    return Failure::Failed;
#else
    BOOST_ASSERT( std::current_exception() );
    return std::current_exception();
#endif // NDEBUG
}

template <typename Exception, typename ... Args>
Failure failure( Args ... args ) noexcept
{
#if defined( NDEBUG ) || defined( PLATFORM_IOS ) || ( defined( __ANDROID__ ) && defined( __arm__ ) ) || defined( BOOST_NO_EXCEPTIONS )
    boost::ignore_unused( args... );
    return Failure::Failed;
#else
    return std::make_exception_ptr( Exception( args...) );
#endif // NDEBUG
}

inline boost::err::no_err_t success() noexcept { return boost::err::no_err; }

inline void logError( Failure const & failure ) noexcept
{
#if defined( NDEBUG ) || defined( PLATFORM_IOS ) || ( defined( __ANDROID__ ) && defined( __arm__ ) ) || defined( BOOST_NO_EXCEPTIONS )
    boost::ignore_unused( failure );
    LOGD( "ZXing failure" );
#else
    try { std::rethrow_exception( failure ); }
    catch (std::exception const & e) { LOGD("ZXing failure: %s", e.what()); }
    catch (...)                      { LOGD("ZXing failure"); }
#endif // NDEBUG
}

/// Temporary utility macro for placing before throw expressions to cause a
/// break when the throw would happen - for easier prioritizing which throw
/// statements to eliminate first.
#define MB_ZXING_BREAK_ON_EXCEPTION() BOOST_ASSERT( !"Exception thrown in ZXing code" );

struct DisambiguationWrapper { Failure result; operator Failure && () && { return std::move( result ); } };

//------------------------------------------------------------------------------
} // namespace pping
//------------------------------------------------------------------------------
