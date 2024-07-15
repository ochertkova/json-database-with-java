package client;

import com.beust.jcommander.Parameter;

import java.util.ArrayList;
import java.util.List;

public class Args {
    @Parameter
    private List<String> parameters = new ArrayList<>();

    @Parameter(names = "-t", description = "Type of the request")
    private String requestType = "get";
    @Parameter(names = "-i", description = "The index of the cell")
    private Integer cellIndex= 1;
}
