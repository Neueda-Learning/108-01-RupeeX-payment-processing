import org.springframework.stereotype.Service;

@Service
public class PaymentProcessingServiceImpl
        implements PaymentProcessingService{


    @Autowired
    private PaymentStatusService statusService;



    @Override
    public void processPayment(Long id){


        statusService.updateStatus(
                id,
                PaymentStatus.VALIDATED
        );


        statusService.updateStatus(
                id,
                PaymentStatus.SENT
        );


        statusService.updateStatus(
                id,
                PaymentStatus.COMPLETED
        );


    }


}