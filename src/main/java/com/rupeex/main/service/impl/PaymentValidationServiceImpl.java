@Service
public class PaymentValidationServiceImpl
        implements PaymentValidationService{


    @Override
    public void validate(
            PaymentRequest request){


        if(request.getAmount()
                .compareTo(BigDecimal.ZERO)<=0){

            throw new InvalidPaymentException(
                    "Invalid amount"
            );

        }


        if(request.getSourceAccount()
                .equals(request.getDestinationAccount())){

            throw new InvalidPaymentException(
                    "Same account"
            );

        }


    }


}