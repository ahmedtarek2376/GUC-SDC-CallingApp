package com.guc.ahmed.callingapp.route;

import java.util.List;

//. by Haseem Saheed
public interface Parser {
    List<Route> parse() throws RouteException;
}