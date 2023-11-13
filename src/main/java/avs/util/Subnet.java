package avs.util;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;


/**
 * @author c3oe.de, based on snippets from Scott Plante, John Kugelmann
 * 
 * Taken from this StackOverflow post: https://stackoverflow.com/a/25165891
 */
public class Subnet
{
    final private int bytesSubnetCount;
    final private BigInteger bigMask;
    final private BigInteger bigSubnetMasked;
    final private String address;
    
    /** For use via format "192.168.0.0/24" or "2001:db8:85a3:880:0:0:0:0/57" */
    public Subnet( final InetAddress subnetAddress, final int bits )
    {
        this.bytesSubnetCount = subnetAddress.getAddress().length; // 4 or 16
        this.bigMask = BigInteger.valueOf( -1 ).shiftLeft( this.bytesSubnetCount*8 - bits ); // mask = -1 << 32 - bits
        this.bigSubnetMasked = new BigInteger( subnetAddress.getAddress() ).and( this.bigMask );
        this.address = bigInteger2IpString( this.bigSubnetMasked, this.bytesSubnetCount );
    }

    /** For use via format "192.168.0.0/255.255.255.0" or single address */
    public Subnet( final InetAddress subnetAddress, final InetAddress mask )
    {
        this.bytesSubnetCount = subnetAddress.getAddress().length;
        this.bigMask = null == mask ? BigInteger.valueOf( -1 ) : new BigInteger( mask.getAddress() ); // no mask given case is handled here.
        this.bigSubnetMasked = new BigInteger( subnetAddress.getAddress() ).and( this.bigMask );
        this.address = bigInteger2IpString( this.bigSubnetMasked, this.bytesSubnetCount );
    }

    /**
     * Subnet factory method.
     * @param subnetAndMask format: "192.168.0.0/24" or "192.168.0.0/255.255.255.0"
     *      or single address or "2001:db8:85a3:880:0:0:0:0/57"
     * @return a new instance
     * @throws UnknownHostException thrown if unsupported subnet mask.
     */
    public static Subnet createInstance( final String subnetAndMask )
    {
        try {
            final String[] stringArr = subnetAndMask.split("/");
            if ( 2 > stringArr.length )
                return new Subnet( InetAddress.getByName( stringArr[ 0 ] ), (InetAddress)null );
            else if ( stringArr[ 1 ].contains(".") || stringArr[ 1 ].contains(":") )
                return new Subnet( InetAddress.getByName( stringArr[ 0 ] ), InetAddress.getByName( stringArr[ 1 ] ) );
            else
                return new Subnet( InetAddress.getByName( stringArr[ 0 ] ), Integer.parseInt( stringArr[ 1 ] ) );     
        } catch ( final UnknownHostException e ) {
            return null;
        }
    }

    public boolean isInNet( final String address ) { 
      try {
        return isInNet(InetAddress.getByName(address));
      } catch (UnknownHostException e) {
        return false;
      } 
    }
    
    public boolean isInNet( final InetAddress address )
    {
        final byte[] bytesAddress = address.getAddress();
        if ( this.bytesSubnetCount != bytesAddress.length )
            return false;
        return new BigInteger( bytesAddress ).and( this.bigMask ).equals( this.bigSubnetMasked );
    }

    
    public boolean partialEquals( final String other ) {
      return address.equals(other);
    }    
    
    public boolean partialEquals( final Subnet other ) {
      return this.bigSubnetMasked.equals(other.bigSubnetMasked);
    }
    
    @Override
    final public boolean equals( Object obj )
    {
        if ( ! (obj instanceof Subnet) )
            return false;
        final Subnet other = (Subnet)obj;
        return  this.bigSubnetMasked.equals( other.bigSubnetMasked ) &&
                this.bigMask.equals( other.bigMask ) &&
                this.bytesSubnetCount == other.bytesSubnetCount;
    }

    @Override
    final public int hashCode()
    {
        return this.bytesSubnetCount;
    }

    @Override
    public String toString()
    {
        return address + "/" + bigInteger2IpString( this.bigMask, this.bytesSubnetCount );
    }

    static public String bigInteger2IpString( final BigInteger bigInteger, final int displayBytes )
    {
        String ip = "";
        final boolean isIPv4 = 4 == displayBytes;
        byte[] bytes = bigInteger.toByteArray();
        int diffLen = displayBytes - bytes.length;
        final byte fillByte = 0 > (int)bytes[ 0 ] ? (byte)0xFF : (byte)0x00;

        int integer;
        for ( int i = 0; i < displayBytes; i++ )
        {
            if ( 0 < i && ! isIPv4 && i % 2 == 0 )
                ip += ':' ;
            else if ( 0 < i && isIPv4 )
                ip += '.';
            integer = 0xFF & (i < diffLen ? fillByte : bytes[ i - diffLen ]);
            if ( ! isIPv4 && 0x10 > integer )
                ip += '0';
            ip += isIPv4 ? integer : Integer.toHexString( integer );
        }
        
        return ip;
    }
}