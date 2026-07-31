@Service
public class PaymentStatusServiceImpl
        implements PaymentStatusService{


    @Override
    public boolean isValidTransition(
            PaymentStatus oldStatus,
            PaymentStatus newStatus){


        return switch(oldStatus){

            case CREATED ->
                    newStatus == PaymentStatus.VALIDATED ||
                            newStatus == PaymentStatus.FAILED;


            case VALIDATED ->
                    newStatus == PaymentStatus.SENT ||
                            newStatus == PaymentStatus.FAILED;


            case SENT ->
                    newStatus == PaymentStatus.COMPLETED ||
                            newStatus == PaymentStatus.FAILED;


            default ->
                    false;

        };

    }


}