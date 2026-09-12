#!/usr/bin/env bash
# OpenAPI Annotation Validation Script
# Validates that all REST controllers have proper OpenAPI annotations
# Returns 0 (success) but prints warnings for missing annotations

set -euo pipefail

echo "🔍 Validating OpenAPI annotations in REST controllers..."

# Find all controller classes
CONTROLLER_FILES=$(find . -path "*/src/main/java/*/controller/*.java" -name "*Controller.java" 2>/dev/null | grep -v target | sort -u)

if [ -z "$CONTROLLER_FILES" ]; then
    echo "⚠️  No controller files found"
    exit 0
fi

echo "Found controller files:"
echo "$CONTROLLER_FILES"

WARNINGS=0

for controller in $CONTROLLER_FILES; do
    echo ""
    echo "📋 Checking $controller..."
    
    # Check for @RestController or @Controller
    if ! grep -q "@RestController\|@Controller" "$controller"; then
        echo "⚠️  $controller: Missing @RestController or @Controller annotation"
        WARNINGS=$((WARNINGS + 1))
    fi
    
    # Check for @RequestMapping or @GetMapping/@PostMapping/etc.
    if ! grep -q "@RequestMapping\|@GetMapping\|@PostMapping\|@PutMapping\|@DeleteMapping\|@PatchMapping" "$controller"; then
        echo "⚠️  $controller: Missing @RequestMapping or HTTP method mapping annotations"
        WARNINGS=$((WARNINGS + 1))
    fi
    
    # Check for @Operation or @ApiOperation (OpenAPI operation documentation)
    if ! grep -q "@Operation\|@ApiOperation" "$controller"; then
        echo "⚠️  $controller: Missing @Operation/@ApiOperation for API documentation"
        WARNINGS=$((WARNINGS + 1))
    fi
    
    # Check for @Tag or @Api (API grouping)
    if ! grep -q "@Tag\|@Api(" "$controller"; then
        echo "⚠️  $controller: Missing @Tag/@Api for API grouping"
        WARNINGS=$((WARNINGS + 1))
    fi
    
    # Check for @Parameter or @ApiParam on method parameters
    if ! grep -q "@Parameter\|@ApiParam" "$controller"; then
        echo "⚠️  $controller: Missing @Parameter/@ApiParam for parameter documentation"
        WARNINGS=$((WARNINGS + 1))
    fi
    
    # Check for @ApiResponses or @ApiResponse
    if ! grep -q "@ApiResponses\|@ApiResponse" "$controller"; then
        echo "⚠️  $controller: Missing @ApiResponses/@ApiResponse for response documentation"
        WARNINGS=$((WARNINGS + 1))
    fi
    
    if [ $WARNINGS -eq 0 ]; then
        echo "✅ $controller has basic OpenAPI annotations"
    fi
done

if [ $WARNINGS -gt 0 ]; then
    echo ""
    echo "⚠️  OpenAPI annotation validation completed with $WARNINGS warnings"
    echo "💡 Consider adding @Operation, @Tag, @Parameter, @ApiResponse annotations to improve API documentation"
    echo "✅ Validation passed (warnings only, not failing build)"
else
    echo ""
    echo "✅ All controllers have basic OpenAPI annotations"
fi

exit 0