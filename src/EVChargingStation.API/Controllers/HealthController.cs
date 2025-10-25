using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Logging;

namespace EVChargingStation.API.Controllers
{
    [ApiController]
    [Route("[controller]")]
    public class HealthController : ControllerBase
    {
        private readonly ILogger<HealthController> _logger;

        public HealthController(ILogger<HealthController> logger)
        {
            _logger = logger;
        }

        [HttpGet]
        public IActionResult Get()
        {
            _logger.LogInformation("Health endpoint was called.");
            return Ok("Healthy");
        }
    }
}
